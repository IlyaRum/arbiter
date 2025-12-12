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
  private final Map<String, Instant> unitCurrentTimestamps = new ConcurrentHashMap<>();

  private final Map<String, Map<String, Double>> unitPreviousParameterValues = new ConcurrentHashMap<>();
  private final Map<String, Map<String, Double>> unitPreviousTopologyValues = new ConcurrentHashMap<>();
  private final Map<String, Map<String, Double>> unitPreviousElementValues = new ConcurrentHashMap<>();
  private final Map<String, Map<String, Double>> unitPreviousFactorValues = new ConcurrentHashMap<>();
  private final Map<String, Map<String, Double>> unitPreviousRepairValues = new ConcurrentHashMap<>();

  // Мапы для накопления изменений типов данных для каждого сечения
  private final Map<String, Map<String, Parameter>> unitAccumulatedChanges = new ConcurrentHashMap<>();
  private final Map<String, Map<String, Topology>> unitAccumulatedTopologyChanges = new ConcurrentHashMap<>();
  private final Map<String, Map<String, Element>> unitAccumulatedElementChanges = new ConcurrentHashMap<>();
  private final Map<String, Map<String, InfluencingFactor>> unitAccumulatedFactorChanges = new ConcurrentHashMap<>();
  private final Map<String, Map<String, Composition>> unitAccumulatedRepairChanges = new ConcurrentHashMap<>();

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

      Map<String, Parameter> accumulatedChanges = unitAccumulatedChanges.get(unitId);
      Map<String, Topology> accumulatedTopologyChanges = unitAccumulatedTopologyChanges.get(unitId);
      Map<String, Element> accumulatedElementChanges = unitAccumulatedElementChanges.get(unitId);
      Map<String, InfluencingFactor> accumulatedInfluencingFactorChanges = unitAccumulatedFactorChanges.get(unitId);

      // Собираем полученные измерения для этого юнита
      Set<String> receivedUids = new HashSet<>();
      Instant currentBatchTimestamp = null;

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
            logger.debug("Updated buffer for unit=" + unitId + ", uid=" + receivedUid + ", value=" + measurement.getValue());
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
              saveCurrentTopologyValues(unitId, result);
              saveCurrentElementValues(unitId, result);
              saveCurrentInfluencingFactorValues(unitId, result);
              logger.debug("Initial data loaded for unit " + unitId);

              // Для первого раза отправляем все данные
              if (firstTime && dataReadyCallback != null) {
                dataReadyCallback.onDataReady(result, unitId);
                firstTime = false;
              }
            } else {
              // Накапливаем изменения
              accumulateChanges(unitId, result);
              accumulateTopologyChanges(unitId, result);
              accumulateElementChanges(unitId, result);
              accumulateInfluencingFactorChanges(unitId, result);

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
                  saveCurrentTopologyValuesFromAccumulated(unitId);
                  saveCurrentElementValuesFromAccumulated(unitId);
                  saveCurrentInfluencingFactorValuesFromAccumulated(unitId);

                  // Очищаем накопленные изменения
                  accumulatedChanges.clear();
                  accumulatedTopologyChanges.clear();
                  accumulatedElementChanges.clear();
                  accumulatedInfluencingFactorChanges.clear();
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
    unitCurrentTimestamps.putIfAbsent(unitId, Instant.MIN);

    unitPreviousParameterValues.computeIfAbsent(unitId, k -> new ConcurrentHashMap<>());
    unitPreviousTopologyValues.computeIfAbsent(unitId, k -> new ConcurrentHashMap<>());
    unitPreviousElementValues.computeIfAbsent(unitId, k -> new ConcurrentHashMap<>());
    unitPreviousFactorValues.computeIfAbsent(unitId, k -> new ConcurrentHashMap<>());


    unitAccumulatedChanges.computeIfAbsent(unitId, k -> new ConcurrentHashMap<>());
    unitAccumulatedTopologyChanges.computeIfAbsent(unitId, k -> new ConcurrentHashMap<>());
    unitAccumulatedElementChanges.computeIfAbsent(unitId, k -> new ConcurrentHashMap<>());
    unitAccumulatedFactorChanges.computeIfAbsent(unitId, k -> new ConcurrentHashMap<>());
  }

  /**
   * Накапливает изменения параметров для конкретного юнита
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
   * Накапливает изменения топологий для конкретного юнита
   */
  private void accumulateTopologyChanges(String unitId, StoreData result) {
    Map<String, Topology> accumulatedTopologyChanges = unitAccumulatedTopologyChanges.get(unitId);
    UnitDto unitDto = findUnitDtoById(result, unitId);
    if (unitDto == null) return;

    for (Topology topology : unitDto.getTopologyList()) {
      String topologyId = topology.getId();
      double currentValue = topology.getValue();

      if (hasTopologyValueChanged(unitId, topologyId, currentValue)) {
        accumulatedTopologyChanges.put(topologyId, topology);
        logger.debug("Topology changed for unit " + unitId + ": " +
          topology.getName() + " = " + currentValue);
      }
    }
  }

  /**
   * Накапливает изменения элементов для конкретного юнита
   */
  private void accumulateElementChanges(String unitId, StoreData result) {
    Map<String, Element> accumulatedElementChanges = unitAccumulatedElementChanges.get(unitId);
    UnitDto unitDto = findUnitDtoById(result, unitId);
    if (unitDto == null) return;

    for (Element element : unitDto.getElements()) {
      String elementId = element.getId();
      double currentValue = element.getValue();

      if (hasElementValueChanged(unitId, elementId, currentValue)) {
        accumulatedElementChanges.put(elementId, element);
        logger.debug("Element changed for unit " + unitId + ": " +
          element.getName() + " = " + currentValue);
      }
    }
  }

  /**
   * Накапливает изменения элементов для конкретного юнита
   */
  private void accumulateInfluencingFactorChanges(String unitId, StoreData result) {
    Map<String, InfluencingFactor> accumulatedInfluencingFactorChanges = unitAccumulatedFactorChanges.get(unitId);
    UnitDto unitDto = findUnitDtoById(result, unitId);
    if (unitDto == null) return;

    for (InfluencingFactor influencingFactor : unitDto.getInfluencingFactors()) {
      String influencingFactorId = influencingFactor.getId();
      double currentValue = influencingFactor.getValue();

      if (hasFactorValueChanged(unitId, influencingFactorId, currentValue)) {
        accumulatedInfluencingFactorChanges.put(influencingFactorId, influencingFactor);
        logger.debug("InfluencingFactor changed for unit " + unitId + ": " +
          influencingFactor.getName() + " = " + currentValue);
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
    Map<String, Topology> accumulatedTopologyChanges = unitAccumulatedTopologyChanges.get(unitId);
    Map<String, Element> accumulatedElementChanges = unitAccumulatedElementChanges.get(unitId);
    Map<String, InfluencingFactor> accumulatedInfluencingFactorChanges = unitAccumulatedFactorChanges.get(unitId);

    if (!accumulatedChanges.isEmpty() ||
      !accumulatedTopologyChanges.isEmpty() ||
      !accumulatedElementChanges.isEmpty() ||
      !accumulatedInfluencingFactorChanges.isEmpty()) {
      Unit unit = findUnitByName(unitId);
      if (unit != null) {
        Map<String, Parameter> changesForUnit = new HashMap<>();
        Map<String, Topology> topologyChangesForUnit = new HashMap<>();
        Map<String, Element> elementChangesForUnit = new HashMap<>();
        Map<String, InfluencingFactor> influencingFactorChangesForUnit = new HashMap<>();

        for (Parameter param : accumulatedChanges.values()) {
          if (isParameterBelongsToUnit(unit, param)) {
            changesForUnit.put(getMappedParameterKey(param), param);
          }
        }

        for (Topology topology : accumulatedTopologyChanges.values()) {
          if (isTopologyBelongsToUnit(unit, topology)) {
            topologyChangesForUnit.put(topology.getId(), topology);
          }
        }

        for (Element element : accumulatedElementChanges.values()) {
          if (isElementBelongsToUnit(unit, element)) {
            elementChangesForUnit.put(element.getId(), element);
          }
        }

        for (InfluencingFactor influencingFactor : accumulatedInfluencingFactorChanges.values()) {
          if (isInfluencingFactorBelongsToUnit(unit, influencingFactor)) {
            influencingFactorChangesForUnit.put(influencingFactor.getId(), influencingFactor);
          }
        }

        if (!changesForUnit.isEmpty() ||
          !topologyChangesForUnit.isEmpty() ||
          !elementChangesForUnit.isEmpty() ||
          !influencingFactorChangesForUnit.isEmpty()) {

          FilteredUnitDto unitDto = new FilteredUnitDto(new UnitDto(unit),
                  changesForUnit,
                  topologyChangesForUnit,
                  elementChangesForUnit,
                  influencingFactorChangesForUnit);
          accumulatedResult.addUnitData(unitDto);

          logger.debug("Created StoreData with " + changesForUnit.size() +
            " changed parameters and " + elementChangesForUnit.size() +
            " changed elements and " + topologyChangesForUnit.size() +
            " changed topologies and " + influencingFactorChangesForUnit.size() +
            " changed influencingFactor for unit: " + unitId);
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
   * Проверяет принадлежность топологии юниту
   */
  private boolean isTopologyBelongsToUnit(Unit unit, Topology topology) {
    for (Topology unitTopology : unit.getTopologies()) {
      if (unitTopology.getId().equals(topology.getId())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Проверяет принадлежность Element юниту
   */
  private boolean isElementBelongsToUnit(Unit unit, Element element) {
    for (Element unitElement : unit.getElements()) {
      if (unitElement.getId().equals(element.getId())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Проверяет принадлежность InfluencingFactor юниту
   */
  private boolean isInfluencingFactorBelongsToUnit(Unit unit, InfluencingFactor influencingFactor) {
    for (InfluencingFactor unitInfluencingFactor : unit.getInfluencingFactors()) {
      if (unitInfluencingFactor.getId().equals(influencingFactor.getId())) {
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
   * Сохраняет текущие значения топологий из накопленных изменений
   */
  private void saveCurrentTopologyValuesFromAccumulated(String unitId) {
    Map<String, Topology> accumulatedTopologyChanges = unitAccumulatedTopologyChanges.get(unitId);
    Map<String, Double> previousTopologyValues = unitPreviousTopologyValues.get(unitId);

    for (Topology topology : accumulatedTopologyChanges.values()) {
      previousTopologyValues.put(topology.getId(), topology.getValue());
      logger.debug("Updated previous topology value for unit " + unitId +
        ": " + topology.getName() + " = " + topology.getValue());
    }
  }

  /**
   * Сохраняет текущие значения элементов из накопленных изменений
   */
  private void saveCurrentElementValuesFromAccumulated(String unitId) {
    Map<String, Element> accumulatedElementChanges = unitAccumulatedElementChanges.get(unitId);
    Map<String, Double> previousElementValues = unitPreviousElementValues.get(unitId);

    for (Element element : accumulatedElementChanges.values()) {
      previousElementValues.put(element.getId(), element.getValue());
      logger.debug("Updated previous element value for unit " + unitId +
        ": " + element.getName() + " = " + element.getValue());
    }
  }

  /**
   * Сохраняет текущие значения InfluencingFactor из накопленных изменений
   */
  private void saveCurrentInfluencingFactorValuesFromAccumulated(String unitId) {
    Map<String, InfluencingFactor> accumulatedInfluencingFactorChanges = unitAccumulatedFactorChanges.get(unitId);
    Map<String, Double> previousInfluencingFactorValues = unitPreviousFactorValues.get(unitId);

    for (InfluencingFactor influencingFactor : accumulatedInfluencingFactorChanges.values()) {
      previousInfluencingFactorValues.put(influencingFactor.getId(), influencingFactor.getValue());
      logger.debug("Updated previous influencingFactor value for unit " + unitId +
        ": " + influencingFactor.getName() + " = " + influencingFactor.getValue());
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
   * Сохраняет текущие значения топологий для конкретного юнита
   */
  private void saveCurrentTopologyValues(String unitId, StoreData result) {
    Map<String, Double> previousTopologyValues = unitPreviousTopologyValues.get(unitId);
    UnitDto unitDto = findUnitDtoById(result, unitId);
    if (unitDto != null) {
      for (Topology topology : unitDto.getTopologyList()) {
        double currentValue = topology.getValue();
        previousTopologyValues.put(topology.getId(), currentValue);
        logger.debug("Saved initial topology value for unit " + unitId +
          ": " + topology.getName() + " = " + currentValue);
      }
    }
  }

  /**
   * Сохраняет текущие значения элементов для конкретного юнита
   */
  private void saveCurrentElementValues(String unitId, StoreData result) {
    Map<String, Double> previousElementValues = unitPreviousElementValues.get(unitId);
    UnitDto unitDto = findUnitDtoById(result, unitId);
    if (unitDto != null) {
      for (Element element : unitDto.getElements()) {
        double currentValue = element.getValue();
        previousElementValues.put(element.getId(), currentValue);
        logger.debug("Saved initial element value for unit " + unitId +
          ": " + element.getName() + " = " + currentValue);
      }
    }
  }

  /**
   * Сохраняет текущие значения InfluencingFactor для конкретного юнита
   */
  private void saveCurrentInfluencingFactorValues(String unitId, StoreData result) {
    Map<String, Double> previousInfluencingFactorValues = unitPreviousFactorValues.get(unitId);
    UnitDto unitDto = findUnitDtoById(result, unitId);
    if (unitDto != null) {
      for (InfluencingFactor influencingFactor : unitDto.getInfluencingFactors()) {
        double currentValue = influencingFactor.getValue();
        previousInfluencingFactorValues.put(influencingFactor.getId(), currentValue);
        logger.debug("Saved initial influencingFactor value for unit " + unitId +
          ": " + influencingFactor.getName() + " = " + currentValue);
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

    // Особый случай: если значение 99999 - всегда считаем изменением
    //TODO[IER] На тестировании обнаружилось для 'МДП с ПА [СМЗУ]' и 'АДП [СМЗУ]' всегда равны 99999.0
    // поставил условие, чтобы всегда попадало в PUT запрос
    if (currentValue == 99999.0 || previousValue == 99999.0) {
      return true;
    }

    // Сравниваем с учетом точности double
    return Math.abs(currentValue - previousValue) > 1e-10;
  }

  /**
   * Проверяет, изменилось ли значение топологии
   */
  private boolean hasTopologyValueChanged(String unitId, String topologyId, double currentValue) {
    Map<String, Double> previousTopologyValues = unitPreviousTopologyValues.get(unitId);
    Double previousValue = previousTopologyValues.get(topologyId);

    if (previousValue == null) {
      return true;
    }

    return Math.abs(currentValue - previousValue) > 1e-10;
  }

  /**
   * Проверяет, изменилось ли значение элемента
   */
  private boolean hasElementValueChanged(String unitId, String elementId, double currentValue) {
    Map<String, Double> previousElementValues = unitPreviousElementValues.get(unitId);
    Double previousValue = previousElementValues.get(elementId);

    if (previousValue == null) {
      return true;
    }

    return Math.abs(currentValue - previousValue) > 1e-10;
  }

  /**
   * Проверяет, изменилось ли значение InfluencingFactor
   */
  private boolean hasFactorValueChanged(String unitId, String elementId, double currentValue) {
    Map<String, Double> previousFactorValues = unitPreviousFactorValues.get(unitId);
    Double previousValue = previousFactorValues.get(elementId);

    if (previousValue == null) {
      return true;
    }

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
   * Класс для UnitDto с фильтрованными параметрами и топологией
   */
  private static class FilteredUnitDto extends UnitDto {
    private final Map<String, Parameter> filteredParameters;
    private final Map<String, Topology> filteredTopologies;
    private final List<Topology> filteredTopologyList;
    private final List<Element> filteredElementList;
    private final Map<String, Element> filteredElements;
    private final List<InfluencingFactor> filteredInfuencingFactorList;
    private final Map<String, InfluencingFactor> influencingFactories;

    public FilteredUnitDto(UnitDto original,
                           Map<String, Parameter> filteredParameters,
                           Map<String, Topology> filteredTopologies,
                           Map<String, Element> filteredElements,
                           Map<String, InfluencingFactor> influencingFactories) {
      super(original.getUnit());
      this.filteredParameters = filteredParameters;
      this.filteredTopologies = filteredTopologies;
      this.filteredTopologyList = new ArrayList<>(filteredTopologies.values());
      this.filteredElements = filteredElements;
      this.influencingFactories = influencingFactories;
      this.filteredElementList = new ArrayList<>(filteredElements.values());
      this.filteredInfuencingFactorList = new ArrayList<>(influencingFactories.values());
    }

    @Override
    public Map<String, Parameter> getParameters() {
      return filteredParameters;
    }

    @Override
    public List<Topology> getTopologyList() {
      return filteredTopologyList;
    }

    @Override public List<Element> getElements() {
      return filteredElementList;
    }

    @Override
    public List<InfluencingFactor> getInfluencingFactors() {
      return filteredInfuencingFactorList;
    }
  }

  public interface DataReadyCallback {
    void onDataReady(StoreData data, String unitId);
  }

}
