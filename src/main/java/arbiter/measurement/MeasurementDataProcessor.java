package arbiter.measurement;

import arbiter.constants.ParameterMappingConstants;
import arbiter.data.*;
import arbiter.di.DependencyInjector;
import arbiter.service.HandleDataService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.cloudevents.jackson.JsonFormat;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;
import io.vertx.ext.web.client.WebClient;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MeasurementDataProcessor {

  private static final Logger logger = LoggerFactory.getLogger(MeasurementDataProcessor.class);

  private boolean firstTime = true;
  private DataReadyCallback dataReadyCallback;
  private final DependencyInjector dependencyInjector;
  private final Map<String, Map<String, Measurement>> unitDataBuffers = new ConcurrentHashMap<>();
  private final Map<String, Map<String, Instant>> unitLastTimeStamps = new ConcurrentHashMap<>();
  private final Map<String, Boolean> unitInitialDataLoaded = new ConcurrentHashMap<>();
  private final Map<String, Map<String, Double>> unitPreviousParameterValues = new ConcurrentHashMap<>();
  private final Map<String, Map<String, Parameter>> unitAccumulatedChanges = new ConcurrentHashMap<>();
  private final Map<String, Instant> unitCurrentTimestamps = new ConcurrentHashMap<>();

  public MeasurementDataProcessor(DependencyInjector dependencyInjector) {
    this.dependencyInjector = dependencyInjector;
  }

  /**
   * Основной метод обработки полученных данных измерений
   */
  public void onDataReceived(MeasurementList list) {
    logger.debug("Processing measurement list with size: " + list.size());

    try {
      StoreData result = processMeasurementsToStoreData(list);

      if (result.size() > 0) {
        //dataProcessor.accept(result);
        dataBatchAggregator(list.getMeasurements(), result);

//        if (firstTime) {
//          //logger.debug(String.format("### получено %d новых значений : %s", result.size(), result));
//          String jsonData = convertStoreDataToJson(Collections.singletonList(result.getUnitDataList()));
//          sendPostRequestAsync(jsonData);
//          firstTime = false;
//        }
      }

    } catch (Exception e) {
      logger.error("Ошибка при обработке данных измерений", e);
    }
  }

  /**
   * Преобразует список измерений в StoreData
   */
  private StoreData processMeasurementsToStoreData(MeasurementList list) {
    StoreData result = new StoreData();

    //TODO[IER]Для разработки. Удалить.
    if (firstTime) {
      logger.debug("measurementList = " + list);
    }

    for (int i = 0; i < list.size(); i++) {
      Measurement measurement = list.get(i);
      MemoryData memoryData = createMemoryData(measurement);

      //store.put(memoryData.getId(), memoryData);

      // Items[j].Parameters.Data[k]
      List<Unit> units = dependencyInjector.getUnitCollection().getUnits();
      for (Unit unit : units) {
        processParameters(memoryData, result, unit);
        processTopologies(memoryData, result, unit);
        processElements(memoryData, result, unit);
        processInfluencingFactors(memoryData, result, unit);
        processRepairSchema(memoryData, result, unit);
      }
    }

    return result;
  }

  /**
   * Устанавливает callback для уведомления о готовых данных
   */
  public void setDataReadyCallback(DataReadyCallback callback) {
    this.dataReadyCallback = callback;
  }

  /**
   * Агрегатор данных для каждого юнита отдельно
   */
  private void dataBatchAggregator(List<Measurement> measurements, StoreData result) {
    // Группируем измерения по UID для быстрого доступа
    logger.debug(String.format("dataBatchAggregator: result=%s", result));
    Map<String, Measurement> measurementsByUid = new HashMap<>();
    for (Measurement measurement : measurements) {
      measurementsByUid.put(measurement.getUid().toLowerCase(), measurement);
    }

    // Обрабатываем каждый юнит отдельно
    for (UnitDto unitDto : result.getUnitDataList()) {
      String unitId = getUnitIdentifier(unitDto);
      Unit unit = findUnitByName(unitId);

      if (unit == null) {
        logger.debug("Unit not found: " + unitId);
        continue;
      }

      // Получаем целевые UID из UnitCollection
      Set<String> targetUids = dependencyInjector.getUnitCollection().getTargetUidsForUnit(unit);

      logger.debug("targetUids=" + targetUids);

      if (targetUids.isEmpty()) {
        logger.debug("No target UIDs configured for unit: " + unitId);
        continue;
      }

      // Ленивая инициализация структур состояния для этого юнита
      initializeUnitStateStructures(unitId);

      // Получаем структуры состояния для этого юнита
      Map<String, Measurement> dataBuffer = unitDataBuffers.get(unitId);
      Map<String, Instant> lastTimeStamps = unitLastTimeStamps.get(unitId);
      boolean initialDataLoaded = unitInitialDataLoaded.get(unitId);
      Map<String, Double> previousParameterValues = unitPreviousParameterValues.get(unitId);
      Map<String, Parameter> accumulatedChanges = unitAccumulatedChanges.get(unitId);

      // Собираем полученные измерения для этого юнита
      Set<String> receivedUids = new HashSet<>();
      Instant currentBatchTimestamp = null;
      boolean hasConsistentTimestamp = true;

      // Проверяем согласованность timestamp в текущей пачке
      for (String targetUid : targetUids) {
        Measurement measurement = measurementsByUid.get(targetUid);
        if (measurement != null) {
          receivedUids.add(targetUid);
          Instant measurementTimestamp = Instant.parse(measurement.getTimeStamp());

          if (currentBatchTimestamp == null) {
            currentBatchTimestamp = measurementTimestamp;
          }

          else if (!currentBatchTimestamp.equals(measurementTimestamp)) {
            hasConsistentTimestamp = false;
            break; // Прерываем при первом несовпадении
          }
        }
      }

      // Если не все измерения получены или timestamp не согласованы - пропускаем
      if (!receivedUids.isEmpty()) {
        // Обновляем dataBuffer для полученных UID
        for (String receivedUid : receivedUids) {
          Measurement measurement = measurementsByUid.get(receivedUid);
          if (measurement != null) {
            dataBuffer.put(receivedUid, measurement);
            lastTimeStamps.put(receivedUid, Instant.parse(measurement.getTimeStamp()));
            logger.debug("Updated buffer for unit " + unitId + ", uid " + receivedUid);
          }
        }

        // Проверяем, есть ли у нас полный набор данных с одинаковым timestamp
        boolean allTargetsHaveData = true;
        Instant commonTimestamp = null;
        boolean allTimestampsMatch = true;

        for (String targetUid : targetUids) {
          Measurement bufferedMeasurement = dataBuffer.get(targetUid);
          if (bufferedMeasurement == null) {
            allTargetsHaveData = false;
            logger.debug("Missing data for unit " + unitId + ", uid " + targetUid);
            break;
          }

          Instant bufferedTimestamp = Instant.parse(bufferedMeasurement.getTimeStamp());
          if (commonTimestamp == null) {
            commonTimestamp = bufferedTimestamp;
          } else if (!commonTimestamp.equals(bufferedTimestamp)) {
            allTimestampsMatch = false;
            logger.debug("Buffer timestamp mismatch for unit " + unitId +
              ": " + commonTimestamp + " vs " + bufferedTimestamp);
            break;
          }
        }

        // Если у нас есть полный набор данных с одинаковым timestamp
        if (allTargetsHaveData && allTimestampsMatch && commonTimestamp != null) {
          logger.debug(String.format("Unit %s - Full set available with timestamp %s",
            unitId, commonTimestamp));

          // Проверяем, изменился ли timestamp с последнего раза
          Instant previousTimestamp = unitCurrentTimestamps.get(unitId);
          boolean timeStampChanged = previousTimestamp == null ||
            !previousTimestamp.equals(commonTimestamp);

          if (timeStampChanged) {
            logger.debug("Timestamp changed for unit " + unitId +
              ": " + previousTimestamp + " -> " + commonTimestamp);
            unitCurrentTimestamps.put(unitId, commonTimestamp);

            // Если это первая загрузка
            if (!initialDataLoaded) {
              unitInitialDataLoaded.put(unitId, true);
              saveCurrentParameterValues(unitId, result);
              logger.debug("Initial data loaded for unit " + unitId);

              // Для первого раза отправляем все данные
              if (firstTime && dataReadyCallback != null) {
                dataReadyCallback.onDataReady(result, unitId);
                firstTime = false;
              }
            } else {
              // Накапливаем изменения
              accumulateChanges(unitId, result);

              // Если есть накопленные изменения - отправляем
              if (!accumulatedChanges.isEmpty()) {
                StoreData accumulatedResult = createStoreDataFromAccumulatedChanges(unitId);

                if (accumulatedResult != null && accumulatedResult.size() > 0) {

                  // Уведомляем слушателей о готовых данных
                  if (dataReadyCallback != null) {
                    dataReadyCallback.onDataReady(accumulatedResult, unitId);
                  }

                  // Сохраняем текущие значения как предыдущие
                  saveCurrentParameterValuesFromAccumulated(unitId);

                  // Очищаем накопленные изменения
                  accumulatedChanges.clear();
                }
              }
            }
          } else {
            logger.debug("Timestamp not changed for unit " + unitId + ": " + commonTimestamp);
          }
        } else {
          logger.debug(String.format("Unit %s - Not ready: allTargetsHaveData=%s, allTimestampsMatch=%s",
            unitId, allTargetsHaveData, allTimestampsMatch));
        }
      }
    }
  }

  /**
   * Ленивая инициализация структур состояния для юнита
   */
  private void initializeUnitStateStructures(String unitId) {
    unitDataBuffers.computeIfAbsent(unitId, k -> new ConcurrentHashMap<>());
    unitLastTimeStamps.computeIfAbsent(unitId, k -> new ConcurrentHashMap<>());
    unitInitialDataLoaded.putIfAbsent(unitId, false);
    unitPreviousParameterValues.computeIfAbsent(unitId, k -> new ConcurrentHashMap<>());
    unitAccumulatedChanges.computeIfAbsent(unitId, k -> new ConcurrentHashMap<>());
    unitCurrentTimestamps.putIfAbsent(unitId, Instant.MIN);
  }

  /**
   * Накапливает изменения для конкретного юнита
   */
  private void accumulateChanges(String unitId, StoreData result) {
    Map<String, Parameter> accumulatedChanges = unitAccumulatedChanges.get(unitId);
    // Находим UnitDto для этого юнита
    UnitDto unitDto = findUnitDtoById(result, unitId);
    if (unitDto == null) return;

    for (Parameter param : unitDto.getParameters().values()) {
      String paramId = param.getId();
      double currentValue = param.getValue();

      if (hasParameterValueChanged(unitId, paramId, currentValue)) {
        accumulatedChanges.put(paramId, param);
        logger.debug("Parameter changed for unit " + unitId + ": " +
          param.getName() + " = " + currentValue);
      }
    }
  }

  /**
   * Находит UnitDto по идентификатору юнита
   */
  private UnitDto findUnitDtoById(StoreData result, String unitId) {
    for (UnitDto unitDto : result.getUnitDataList()) {
      if (getUnitIdentifier(unitDto).equals(unitId)) {
        return unitDto;
      }
    }
    return null;
  }

  /**
   * Создает StoreData из накопленных изменений, когда все условия выполнены:
   * все targetUids получены, одинаковый timestamp, initialDataLoaded, hasConsistentTimestamp
   */
  private StoreData createStoreDataFromAccumulatedChanges(String unitId) {
    StoreData accumulatedResult = new StoreData();
    Map<String, Parameter> accumulatedChanges = unitAccumulatedChanges.get(unitId);

    if (!accumulatedChanges.isEmpty()) {
      Unit unit = findUnitByName(unitId);
      if (unit != null) {
        Map<String, Parameter> changesForUnit = new HashMap<>();

        for (Parameter param : accumulatedChanges.values()) {
          if (isParameterBelongsToUnit(unit, param)) {
            changesForUnit.put(getMappedParameterKey(param), param);
          }
        }

        if (!changesForUnit.isEmpty()) {
          UnitDto unitDto = new FilteredUnitDto(new UnitDto(unit), changesForUnit);
          accumulatedResult.addUnitData(unitDto);
          logger.debug("Created StoreData with " + changesForUnit.size() +
            " changed parameters for unit: " + unitId);
        }
      }
    }

    return accumulatedResult;
  }

  /**
   * Находит юнит по имени
   */
  private Unit findUnitByName(String unitName) {
    List<Unit> units = dependencyInjector.getUnitCollection().getUnits();
    for (Unit unit : units) {
      if (unit.getName().equals(unitName)) {
        return unit;
      }
    }
    return null;
  }

  /**
   * Проверяет принадлежность параметра юниту
   */
  private boolean isParameterBelongsToUnit(Unit unit, Parameter param) {
    for (Parameter unitParam : unit.getParameters()) {
      if (unitParam.getId().equals(param.getId())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Сохраняем текущие значения как предыдущие для следующего сравнения для конкретного юнита
   */
  private void saveCurrentParameterValuesFromAccumulated(String unitId) {
    Map<String, Parameter> accumulatedChanges = unitAccumulatedChanges.get(unitId);
    Map<String, Double> previousParameterValues = unitPreviousParameterValues.get(unitId);
    for (Parameter param : accumulatedChanges.values()) {
      previousParameterValues.put(param.getId(), param.getValue());
      logger.debug("Updated previous value for unit " + unitId +
        ": " + param.getName() + " = " + param.getValue());
    }
  }

  /**
   * Получает ключ для маппинга параметра (аналогично getMappedParameterKey в UnitDto)
   */
  private String getMappedParameterKey(Parameter param) {
    //TODO [IER] Используем ту же логику, что и в UnitDto
    // Нужно отрефакторить, чтобы использовать один метод
    return ParameterMappingConstants.PARAMETER_NAME_TO_FIELD_MAPPING.getOrDefault(param.getName(), param.getId());
  }

  /**
   * Находит юнит для параметра
   */
  private Unit findUnitForParameter(Parameter param) {
    List<Unit> units = dependencyInjector.getUnitCollection().getUnits();
    for (Unit unit : units) {
      for (Parameter unitParam : unit.getParameters()) {
        if (unitParam.getId().equals(param.getId())) {
          return unit;
        }
      }
    }
    return null;
  }

  /**
   * Сохраняет текущие значения параметров для конкретного юнита
   */
  private void saveCurrentParameterValues(String unitId, StoreData result) {
    Map<String, Double> previousParameterValues = unitPreviousParameterValues.get(unitId);
    UnitDto unitDto = findUnitDtoById(result, unitId);
    if (unitDto != null) {
      for (Parameter param : unitDto.getParameters().values()) {
        String paramId = param.getId();
        double currentValue = param.getValue();
        previousParameterValues.put(paramId, currentValue);
        logger.debug("Saved initial value for unit " + unitId +
          ": " + param.getName() + " = " + currentValue);
      }
    }
  }

  /**
   * Проверяет, изменилось ли значение параметра.
   * Сравнивает текущее значение с предыдущим сохраненным значением
   */
  private boolean hasParameterValueChanged(String unitId, String paramId, double currentValue) {
    Map<String, Double> previousParameterValues = unitPreviousParameterValues.get(unitId);
    Double previousValue = previousParameterValues.get(paramId);
    // Если предыдущего значения нет - считаем что изменилось (первый раз)
    if (previousValue == null) {
      return true;
    }

    // Сравниваем с учетом точности double
    return Math.abs(currentValue - previousValue) > 1e-10;
  }

  private MemoryData createMemoryData(Measurement measurement) {
    String id = measurement.getUid();
    double value = measurement.getValue();
    Instant time = Instant.parse(measurement.getTimeStamp());
    int qCode = measurement.getQCode();

    return new MemoryData(id, value, time, qCode);
  }

  private void processParameters(MemoryData memoryData, StoreData result, Unit unit) {
    List<Parameter> parameters = unit.getParameters();


    // Аналог: for k := 0 to Items[j].Parameters.Count - 1 do
    for (Parameter parameter : parameters) {

      // Аналог: if CompareText(P.Id, Data.Id) = 0 then
      if (parameter.getId().equalsIgnoreCase(memoryData.getId())) {

        // Проверяем, изменились ли данные
        // Аналог: if not P.Assigned or (P.Time <> Data.Time) or (P.Value <> Data.Value) then
        boolean isDataDifferent = parameter.isDataDifferent(memoryData.getValue(), memoryData.getTime());
//        logger.debug("parameterId=" + parameter.getId() +
//          " / parameterName=" + parameter.getName() +
//          " / parameterValue=" + parameter.getValue() +
//
//          " /----/ memoryData=" + memoryData + " / isDataDifferent=" + isDataDifferent);
        if (isDataDifferent) {

          // Аналог: P.SetData(Data.Value, Data.Time, Data.QCode)
          parameter.setData(memoryData.getValue(), memoryData.getTime(), memoryData.getQCode());

          // Создаем или получаем UnitData для текущего юнита
          UnitDto unitDto = result.getUnitData(unit);
          if (unitDto == null) {
            unitDto = new UnitDto(unit);
            result.addUnitData(unitDto);
          }

          //unitDto.addParameter(parameter);
        }
//          logger.debug(String.format("%s: %s/%s= %f [%s] %s",
//            unit.getName(), parameter.getId(), parameter.getName(), memoryData.getValue(),
//            Integer.toHexString(memoryData.getQCode()),
//            memoryData.getTime().toString()));
        return;
      }
    }
  }

  private void processTopologies(MemoryData memoryData, StoreData result, Unit unit) {

    List<Topology> topologyList = unit.getTopologies();


    for (Topology topology : topologyList) {

      if (topology.getId().equalsIgnoreCase(memoryData.getId())) {

        if (topology.isDataDifferent(memoryData.getValue(), memoryData.getTime())) {

          topology.setData(memoryData.getValue(), memoryData.getTime(), memoryData.getQCode());

          UnitDto unitDto = result.getUnitData(unit);
          if (unitDto == null) {
            unitDto = new UnitDto(unit);
            result.addUnitData(unitDto);
          }
        }
        return;
      }
    }
  }

  private void processElements(MemoryData memoryData, StoreData result, Unit unit) {
    List<Element> elements = unit.getElements();

    for (Element element : elements) {

      if (element.getId().equalsIgnoreCase(memoryData.getId())) {

        if (element.isDataDifferent(memoryData.getValue(), memoryData.getTime())) {

          element.setData(memoryData.getValue(), memoryData.getTime(), memoryData.getQCode());

          UnitDto unitDto = result.getUnitData(unit);
          if (unitDto == null) {
            unitDto = new UnitDto(unit);
            result.addUnitData(unitDto);
          }
        }
        return;
      }
    }
  }

  private void processRepairSchema(MemoryData memoryData, StoreData result, Unit unit) {
    Optional.ofNullable(unit.getRepairSchema())
      .map(RepairSchema::getRepairGroupValues)
      .orElse(Collections.emptyList())
      .forEach(repairGroupValue -> processRepairGroupValue(memoryData, result, unit, repairGroupValue));
  }

  private void processRepairGroupValue(MemoryData memoryData, StoreData result, Unit unit, RepairGroupValue repairGroupValue) {
    List<Composition> compositions = repairGroupValue.getValues();
    for (Composition composition : compositions) {
      if (composition.getId().equalsIgnoreCase(memoryData.getId())) {
        if (composition.isDataDifferent(memoryData.getValue(), memoryData.getTime())) {
          composition.setData(memoryData.getValue(), memoryData.getTime(), memoryData.getQCode());
          UnitDto unitDto = result.getUnitData(unit);
          if (unitDto == null) {
            unitDto = new UnitDto(unit);
            result.addUnitData(unitDto);
          }
        }
        return;
      }
    }
  }

  private void processInfluencingFactors(MemoryData memoryData, StoreData result, Unit unit) {
    List<InfluencingFactor> influencingFactors = unit.getInfluencingFactors();


    for (InfluencingFactor influencingFactor : influencingFactors) {

      if (influencingFactor.getId().equalsIgnoreCase(memoryData.getId())) {

        if (influencingFactor.isDataDifferent(memoryData.getValue(), memoryData.getTime())) {

          influencingFactor.setData(memoryData.getValue(), memoryData.getTime(), memoryData.getQCode());

          UnitDto unitDto = result.getUnitData(unit);
          if (unitDto == null) {
            unitDto = new UnitDto(unit);
            result.addUnitData(unitDto);
          }
        }
        return;
      }
    }
  }

  /**
   * Получает идентификатор юнита из UnitDto
   */
  private String getUnitIdentifier(UnitDto unitDto) {
    return unitDto.getName();
  }

  /**
   * Класс для UnitDto с фильтрованными параметрами
   */
  private static class FilteredUnitDto extends UnitDto {
    private final Map<String, Parameter> filteredParameters;

    public FilteredUnitDto(UnitDto original, Map<String, Parameter> filteredParameters) {
      super(original.getUnit());
      this.filteredParameters = filteredParameters;
    }

    @Override
    public Map<String, Parameter> getParameters() {
      return filteredParameters;
    }
  }

  public interface DataReadyCallback {
    void onDataReady(StoreData data, String unitId);
  }

}
