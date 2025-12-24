package arbiter.measurement;

import arbiter.constants.ParameterMappingConstants;
import arbiter.data.*;
import arbiter.data.dto.FilteredUnitDto;
import arbiter.data.dto.UnitDto;
import arbiter.data.model.*;
import arbiter.di.DependencyInjector;
import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * Отвечает за сбор, фильтрацию и группировку изменений измерений для каждого сечения, перед отправкой в расчетный сервис
 */

public class BatchAggregator {

  private static final Logger logger = LoggerFactory.getLogger(BatchAggregator.class);

  private final DependencyInjector dependencyInjector;
  private final DataReadyCallback dataReadyCallback;
  private final ExecutorService singleThreadExecutor;

  // Состояние для каждого сечения
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


  public BatchAggregator(DependencyInjector dependencyInjector,
                         DataReadyCallback dataReadyCallback,
                         ExecutorService singleThreadExecutor) {
    this.dependencyInjector = dependencyInjector;
    this.dataReadyCallback = dataReadyCallback;
    this.singleThreadExecutor = singleThreadExecutor;
  }

  /**
   * Агрегатор данных для каждого сечения отдельно
   */
  public void aggregateData(List<Measurement> measurements, StoreData result) {
    logger.debug("Получено measurements: " + measurements.size());
    logger.debug("Количество сечений в StoreData: " + result.getUnitDataList().size());

    logger.debug(String.format("Входные параметры: %s", result));
    Map<String, Measurement> measurementsByUid = new HashMap<>();
    for (Measurement measurement : measurements) {
      measurementsByUid.put(measurement.getUid().toLowerCase(), measurement);
    }

    logger.debug("Измерения сгруппированы по UID. Уникальных UID: " + measurementsByUid.size());

    int unitProcessedCount = 0;
    for (UnitDto unitDto : result.getUnitDataList()) {
      String unitId = getUnitIdentifier(unitDto);
      unitProcessedCount++;
      logger.info(String.format("[%d/%d] Обработка сечения: %s",
        unitProcessedCount, result.getUnitDataList().size(), unitId));
      Unit unit = findUnitByName(unitId);

      if (unit == null) {
        logger.warn("Сечение " + unitId + " не найдено!");
        continue;
      }

      Set<String> targetUids = dependencyInjector.getUnitCollection().getTargetUidsForUnit(unit);
      logger.debug(String.format("Юнит %s: targetUids count=%d", unitId, targetUids.size()));
      logger.debug("Unit=" + unitId + " targetUids=" + targetUids);

      if (targetUids.isEmpty()) {
        logger.warn("Нет target UID для сечения: " + unitId);
        continue;
      }

      // Получаем UID "Номер цикла расчета СМЗУ" для этого сечения
      String cycleNumberUid = dependencyInjector.getUnitCollection().getCycleNumberUidFromUnit(unit);
      logger.debug("UID 'Номер цикла расчета СМЗУ' для сечения " + unitId + ": " + cycleNumberUid);

      initializeUnitStateStructures(unitId);
      logger.debug("Структуры состояния инициализированы для сечения: " + unitId);

      Map<String, Measurement> dataBuffer = unitDataBuffers.get(unitId);
      Map<String, Instant> lastTimeStamps = unitLastTimeStamps.get(unitId);
      boolean initialDataLoaded = unitInitialDataLoaded.get(unitId);

      logger.debug(String.format("Состояние сечения %s: bufferSize=%d, initialDataLoaded=%s",
        unitId, dataBuffer.size(), initialDataLoaded));

      Map<String, Parameter> accumulatedChanges = unitAccumulatedChanges.get(unitId);
      Map<String, Topology> accumulatedTopologyChanges = unitAccumulatedTopologyChanges.get(unitId);
      Map<String, Element> accumulatedElementChanges = unitAccumulatedElementChanges.get(unitId);
      Map<String, InfluencingFactor> accumulatedInfluencingFactorChanges = unitAccumulatedFactorChanges.get(unitId);

      Set<String> receivedUids = new HashSet<>();
      boolean hasCycleNumberInCurrentBatch = false;
      Instant cycleTimestamp = null;
      int measurementsFound = 0;

      logger.debug("Проверка полученных измерений в текущей пачке...");

      // Проверяем, есть ли в текущей пачке UID цикла
      if (cycleNumberUid != null && measurementsByUid.containsKey(cycleNumberUid)) {
        hasCycleNumberInCurrentBatch = true;
        Measurement cycleMeasurement = measurementsByUid.get(cycleNumberUid);
        cycleTimestamp = Instant.parse(cycleMeasurement.getTimeStamp());
        logger.debug(String.format("В текущей пачке получен UID цикла: %s с timestamp: %s",
          cycleNumberUid, cycleTimestamp));
      }

      // Собираем все полученные UID из текущей пачки
      for (String targetUid : targetUids) {
        Measurement measurement = measurementsByUid.get(targetUid);
        if (measurement != null) {
          measurementsFound++;
          receivedUids.add(targetUid);

          if (cycleNumberUid != null && targetUid.equals(cycleNumberUid)) {
            logger.debug(String.format("Получен UID цикла: %s, value=%s, timestamp=%s",
              targetUid, measurement.getValue(), measurement.getTimeStamp()));
          }
        }
      }

      logger.debug(String.format("Для сечения %s найдено measurements: %d из %d targetUids, hasCycleNumberInCurrentBatch=%s",
        unitId, measurementsFound, targetUids.size(), hasCycleNumberInCurrentBatch));

      // Обновляем dataBuffer для полученных UID
      if (!receivedUids.isEmpty()) {
        logger.debug("Обновление dataBuffer для полученных UID...");
        for (String receivedUid : receivedUids) {
          Measurement measurement = measurementsByUid.get(receivedUid);
          if (measurement != null) {
            dataBuffer.put(receivedUid, measurement);
            lastTimeStamps.put(receivedUid, Instant.parse(measurement.getTimeStamp()));
            logger.debug("Updated buffer for unit=" + unitId + ", receivedUid=" + receivedUid +
              ", value=" + measurement.getValue() + ", timestamp=" + measurement.getTimeStamp());
          }
        }
        logger.debug(String.format("Buffer обновлен. Текущий размер buffer=%d для сечения '%s'",
          dataBuffer.size(), unitId));
      }

      boolean canCheckConsistency = false;
      Instant referenceTimestamp = null;

      if (cycleNumberUid != null) {
        Measurement cycleMeasurement = dataBuffer.get(cycleNumberUid);
        if (cycleMeasurement != null) {
          referenceTimestamp = Instant.parse(cycleMeasurement.getTimeStamp());
          logger.debug(String.format("Используем timestamp из буфера для UID цикла %s: %s",
            cycleNumberUid, referenceTimestamp));

          // Проверяем, есть ли полный набор данных
          boolean allTargetsHaveData = true;
          boolean allTimestampsMatch = true;

          for (String targetUid : targetUids) {
            Measurement bufferedMeasurement = dataBuffer.get(targetUid);
            if (bufferedMeasurement == null) {
              allTargetsHaveData = false;
              logger.debug("Missing data in buffer for unit " + unitId + ", uid " + targetUid);
              break;
            }

            Instant bufferedTimestamp = Instant.parse(bufferedMeasurement.getTimeStamp());
            if (!referenceTimestamp.equals(bufferedTimestamp)) {
              allTimestampsMatch = false;
              logger.debug("Buffer timestamp mismatch for unit " + unitId +
                ": reference=" + referenceTimestamp + ", actual=" + bufferedTimestamp +
                " для UID: " + targetUid);
              break;
            }
          }

          canCheckConsistency = allTargetsHaveData && allTimestampsMatch;
          logger.debug(String.format("Проверка согласованности для сечения %s: canCheckConsistency=%s, allTargetsHaveData=%s, allTimestampsMatch=%s",
            unitId, canCheckConsistency, allTargetsHaveData, allTimestampsMatch));
        } else {
          logger.debug("UID цикла еще не получен, откладываем проверку согласованности");
        }
      }

      // Если у нас есть полный набор данных с одинаковым timestamp
      if (canCheckConsistency && referenceTimestamp != null) {
        // Проверяем, изменился ли timestamp с последнего раза
        Instant previousTimestamp = unitCurrentTimestamps.get(unitId);
        boolean timeStampChanged = previousTimestamp == null ||
          !previousTimestamp.equals(referenceTimestamp);

        logger.debug(String.format("Сравнение timestamp: previous=%s, current=%s, changed=%s",
          previousTimestamp, referenceTimestamp, timeStampChanged));

        if (timeStampChanged) {
          logger.debug("Timestamp изменился для сечения " + unitId +
            ": " + previousTimestamp + " -> " + referenceTimestamp);
          unitCurrentTimestamps.put(unitId, referenceTimestamp);

          if (!initialDataLoaded) {
            logger.debug("Первичная загрузка данных для сечения " + unitId);
            unitInitialDataLoaded.put(unitId, true);
            saveCurrentParameterValues(unitId, result);
            saveCurrentTopologyValues(unitId, result);
            saveCurrentElementValues(unitId, result);
            saveCurrentInfluencingFactorValues(unitId, result);
            logger.debug("Начальные данные загружены для сечения " + unitId);
          } else {
            logger.debug("Накопление изменений для сечения " + unitId);
            accumulateChanges(unitId, result);
            accumulateTopologyChanges(unitId, result);
            accumulateElementChanges(unitId, result);
            accumulateInfluencingFactorChanges(unitId, result);

            int paramChanges = accumulatedChanges.size();
            int topologyChanges = accumulatedTopologyChanges.size();
            int elementChanges = accumulatedElementChanges.size();
            int factorChanges = accumulatedInfluencingFactorChanges.size();

            logger.info(String.format("Накопленные изменения для сечения %s: Parameter=%d, Topology=%d, Element=%d, Factor=%d",
              unitId, paramChanges, topologyChanges, elementChanges, factorChanges));

            if (!accumulatedChanges.isEmpty() ||
              !accumulatedTopologyChanges.isEmpty() ||
              !accumulatedElementChanges.isEmpty() ||
              !accumulatedInfluencingFactorChanges.isEmpty()) {

              StoreData accumulatedResult = createStoreDataFromAccumulatedChanges(unitId);

              if (accumulatedResult != null && accumulatedResult.size() > 0) {
                logger.debug(String.format("StoreData создан успешно. Размер: %d", accumulatedResult.size()));

                singleThreadExecutor.submit(() -> {
                  try {
                    if (dataReadyCallback != null) {
                      logger.info("Отправка всех накопленных изменений в расчетный сервис для сечения: " + unitId);
                      dataReadyCallback.onDataReady(accumulatedResult, unitId);
                    }
                  } catch (Exception e) {
                    logger.error("Ошибка при обработке данных в callback", e);
                  }
                });

                saveCurrentParameterValuesFromAccumulated(unitId);
                saveCurrentTopologyValuesFromAccumulated(unitId);
                saveCurrentElementValuesFromAccumulated(unitId);
                saveCurrentInfluencingFactorValuesFromAccumulated(unitId);
                logger.info("Текущие значения сохранены как предыдущие");

                accumulatedChanges.clear();
                accumulatedTopologyChanges.clear();
                accumulatedElementChanges.clear();
                accumulatedInfluencingFactorChanges.clear();
                logger.info("Накопленные изменения очищены \n");
              }
            } else {
              logger.debug("Нет накопленных изменений для отправки");
            }
          }
        } else {
          logger.debug("Timestamp: " + referenceTimestamp + " не изменился для сечения: " + unitId);
        }
      } else {
        if (cycleNumberUid == null) {
          logger.debug("UID цикла не определен для сечения: " + unitId);
        } else if (dataBuffer.get(cycleNumberUid) == null) {
          logger.debug("Ожидаем получение UID цикла для проверки согласованности: " + cycleNumberUid);
        } else {
          logger.debug("Неполный набор данных или несовпадение timestamp для сечения: " + unitId);
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
        Map<String, Parameter> parameterChangesForUnit = new HashMap<>();
        Map<String, Topology> topologyChangesForUnit = new HashMap<>();
        Map<String, Element> elementChangesForUnit = new HashMap<>();
        Map<String, InfluencingFactor> influencingFactorChangesForUnit = new HashMap<>();

        for (Parameter param : accumulatedChanges.values()) {
          if (isParameterBelongsToUnit(unit, param)) {
            parameterChangesForUnit.put(getMappedParameterKey(param), param);
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

        if (!parameterChangesForUnit.isEmpty() ||
          !topologyChangesForUnit.isEmpty() ||
          !elementChangesForUnit.isEmpty() ||
          !influencingFactorChangesForUnit.isEmpty()) {

          FilteredUnitDto unitDto = new FilteredUnitDto(new UnitDto(unit),
            parameterChangesForUnit,
            topologyChangesForUnit,
            elementChangesForUnit,
            influencingFactorChangesForUnit);
          accumulatedResult.addUnitData(unitDto);

          logger.debug("Created StoreData for unit " + unitId + " with: " +
            " changed parameters=" + parameterChangesForUnit.size() +
            " and changed elements=" + elementChangesForUnit.size() +
            " and changed topologies=" + topologyChangesForUnit.size() +
            " and changed influencingFactor=" + influencingFactorChangesForUnit.size());
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
      logger.info("Updated previous value for unit " + unitId +
        ": " + param.getName() + "/" + param.getId() + " = " + param.getValue() +
        ", timestamp=" + param.getTime());
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
   * Получает идентификатор юнита из UnitDto
   */
  private String getUnitIdentifier(UnitDto unitDto) {
    return unitDto.getName();
  }
}
