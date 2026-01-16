package arbiter.measurement;

import arbiter.constants.ParameterMappingConstants;
import arbiter.data.*;
import arbiter.data.dto.FilteredUnitDto;
import arbiter.data.dto.UnitDto;
import arbiter.data.model.*;
import arbiter.di.DependencyInjector;
import arbiter.measurement.state.ConsistencyCheckResult;
import arbiter.measurement.state.ConsistencyStatus;
import arbiter.measurement.state.UnitState;
import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * Отвечает за сбор, фильтрацию и группировку изменений измерений для каждого сечения
 */

public class MeasurementChangeTracker {

  private static final Logger logger = LoggerFactory.getLogger(MeasurementChangeTracker.class);

  private final DependencyInjector dependencyInjector;
  private final DataReadyCallback dataReadyCallback;
  private final ExecutorService singleThreadExecutor;

  // Состояние для каждого сечения
  private final Map<String, Boolean> unitInitialDataLoaded = new ConcurrentHashMap<>();

  private final Map<String, Map<String, Double>> unitPreviousParameterValues = new ConcurrentHashMap<>();
  private final Map<String, Map<String, Double>> unitPreviousTopologyValues = new ConcurrentHashMap<>();
  private final Map<String, Map<String, Double>> unitPreviousElementValues = new ConcurrentHashMap<>();
  private final Map<String, Map<String, Double>> unitPreviousFactorValues = new ConcurrentHashMap<>();
  private final Map<String, Map<String, Double>> unitPreviousRepairValues = new ConcurrentHashMap<>();

  // Мапы для накопления изменений типов данных для каждого сечения
  private final Map<String, Map<String, Parameter>> unitTrackedParameterChanges = new ConcurrentHashMap<>();
  private final Map<String, Map<String, Topology>> unitTrackedTopologyChanges = new ConcurrentHashMap<>();
  private final Map<String, Map<String, Element>> unitTrackedElementChanges = new ConcurrentHashMap<>();
  private final Map<String, Map<String, InfluencingFactor>> unitTrackedFactorChanges = new ConcurrentHashMap<>();
  private final Map<String, Map<String, Composition>> unitTrackedRepairChanges = new ConcurrentHashMap<>();


  public MeasurementChangeTracker(DependencyInjector dependencyInjector,
                                  DataReadyCallback dataReadyCallback,
                                  ExecutorService singleThreadExecutor) {
    this.dependencyInjector = dependencyInjector;
    this.dataReadyCallback = dataReadyCallback;
    this.singleThreadExecutor = singleThreadExecutor;
  }

  /**
   * Метод отслеживает изменение данных измерений сечений
   */
  public void trackAndProcessChanges(List<Measurement> measurements, StoreData result) {
    logger.debug("Получено новых измерений: " + measurements.size());
    logger.debug("Количество сечений с новыми измерениями в StoreData: " + result.getUnitDataList().size());
    logger.debug(String.format("Входные параметры: %s", result));

    Map<String, Measurement> measurementsByUid = createMeasurementsMap(measurements);
    trackChangesForAllUnits(result, measurementsByUid);
  }

  private static Map<String, Measurement> createMeasurementsMap(List<Measurement> measurements) {
    Map<String, Measurement> measurementsByUid = new HashMap<>();
    for (Measurement measurement : measurements) {
      measurementsByUid.put(measurement.getUid().toLowerCase(), measurement);
    }
    return measurementsByUid;
  }

  private void trackChangesForAllUnits(StoreData result, Map<String, Measurement> measurementsByUid) {
    int unitProcessedCount = 0;
    for (UnitDto unitDto : result.getUnitDataList()) {
      unitProcessedCount++;
      trackChangesForUnit(result, unitDto, unitProcessedCount, measurementsByUid);
    }
  }

  private void trackChangesForUnit(StoreData result, UnitDto unitDto, int unitProcessedCount, Map<String, Measurement> measurementsByUid) {
    String unitId = getUnitIdentifier(unitDto);
    int totalCount = result.getUnitDataList().size();
    logger.info(String.format("[%d/%d] Обработка сечения: %s", unitProcessedCount, totalCount, unitId));

    initializeUnitStateStructures(unitId);
    logger.debug("Структуры состояния инициализированы для сечения: " + unitId);

    boolean initialDataLoaded = unitInitialDataLoaded.get(unitId);
    UnitState unitState = getUnitState(unitId);
    processTrackedChanges(result, unitId, initialDataLoaded, unitState);
  }

  private UnitState getUnitState(String unitId) {
    return new UnitState(
      unitTrackedParameterChanges.get(unitId),
      unitTrackedTopologyChanges.get(unitId),
      unitTrackedElementChanges.get(unitId),
      unitTrackedFactorChanges.get(unitId)
    );
  }

  private void processTrackedChanges(StoreData result, String unitId, boolean initialDataLoaded,
                                     UnitState unitState) {
      if (!initialDataLoaded) {
        loadInitialValues(result, unitId);
      } else
        accumulateAndProcessChanges(result, unitId, unitState);
  }

  private void loadInitialValues(StoreData result, String unitId) {
    logger.debug("Первичная загрузка данных для сечения '" + unitId + "'");
    unitInitialDataLoaded.put(unitId, true);
    saveCurrentParameterValues(unitId, result);
    saveCurrentTopologyValues(unitId, result);
    saveCurrentElementValues(unitId, result);
    saveCurrentInfluencingFactorValues(unitId, result);
    logger.debug("Начальные данные загружены для сечения '" + unitId + "'");
  }

  private void accumulateAndProcessChanges(StoreData result, String unitId, UnitState unitState) {
    logger.debug("Накопление изменений для сечения '" + unitId + "'");
    trackParameterChanges(unitId, result);
    trackTopologyChanges(unitId, result);
    trackElementChanges(unitId, result);
    trackInfluencingFactorChanges(unitId, result);

    int paramChanges = unitState.getTrackedChanges().size();
    int topologyChanges = unitState.getTrackedTopologyChanges().size();
    int elementChanges = unitState.getTrackedElementChanges().size();
    int factorChanges = unitState.getTrackedInfluencingFactorChanges().size();

    logger.info(String.format("Накопленные изменения для сечения '%s': Parameter=%d, Topology=%d, Element=%d, Factor=%d",
      unitId, paramChanges, topologyChanges, elementChanges, factorChanges));

    if (!unitState.getTrackedChanges().isEmpty() ||
      !unitState.getTrackedTopologyChanges().isEmpty() ||
      !unitState.getTrackedElementChanges().isEmpty() ||
      !unitState.getTrackedInfluencingFactorChanges().isEmpty()) {

      sendTrackedChanges(unitId, unitState);
    } else {
      logger.debug("Нет накопленных изменений для отправки");
    }
  }

  private void sendTrackedChanges(String unitId, UnitState unitState) {
    StoreData trackedResult = createStoreDataFromTrackedChanges(unitId);

    if (trackedResult != null && trackedResult.size() > 0) {

      if (dataReadyCallback != null) {
        singleThreadExecutor.submit(() -> {
          try {

            dataReadyCallback.onDataReady(trackedResult, unitId);
            logger.debug("Вызов dataReadyCallback для сечения: '" + unitId + "'");

          } catch (Exception e) {
            logger.error("Ошибка при вызове dataReadyCallback для сечения: '" + unitId + "': ", e);
          }
        });
      } else {
        logger.error("DataReadyCallback = null. CallBack не был вызван!");
      }

      saveCurrentValuesFromTracked(unitId);
      clearTrackedChanges(unitState);
    }
  }

  private static void clearTrackedChanges(UnitState unitState) {
    unitState.getTrackedChanges().clear();
    unitState.getTrackedTopologyChanges().clear();
    unitState.getTrackedElementChanges().clear();
    unitState.getTrackedInfluencingFactorChanges().clear();
    logger.info("Накопленные изменения очищены \n");
  }

  private void saveCurrentValuesFromTracked(String unitId) {
    saveCurrentParameterValuesFromTracked(unitId);
    saveCurrentTopologyValuesFromTracked(unitId);
    saveCurrentElementValuesFromTracked(unitId);
    saveCurrentInfluencingFactorValuesFromTracked(unitId);
    logger.info("Текущие значения сохранены как предыдущие");
  }

  private void initializeUnitStateStructures(String unitId) {
    unitInitialDataLoaded.putIfAbsent(unitId, false);

    unitPreviousParameterValues.computeIfAbsent(unitId, k -> new ConcurrentHashMap<>());
    unitPreviousTopologyValues.computeIfAbsent(unitId, k -> new ConcurrentHashMap<>());
    unitPreviousElementValues.computeIfAbsent(unitId, k -> new ConcurrentHashMap<>());
    unitPreviousFactorValues.computeIfAbsent(unitId, k -> new ConcurrentHashMap<>());

    unitTrackedParameterChanges.computeIfAbsent(unitId, k -> new ConcurrentHashMap<>());
    unitTrackedTopologyChanges.computeIfAbsent(unitId, k -> new ConcurrentHashMap<>());
    unitTrackedElementChanges.computeIfAbsent(unitId, k -> new ConcurrentHashMap<>());
    unitTrackedFactorChanges.computeIfAbsent(unitId, k -> new ConcurrentHashMap<>());
  }

  /**
   * Накапливает изменения параметров для конкретного юнита
   */
  private void trackParameterChanges(String unitId, StoreData result) {
    Map<String, Parameter> TrackedChanges = unitTrackedParameterChanges.get(unitId);
    UnitDto unitDto = findUnitDtoById(result, unitId);
    if (unitDto == null) return;

    for (Parameter param : unitDto.getParameters().values()) {
      String paramId = param.getId();
      double currentValue = param.getValue();

      if (isParameterChanged(unitId, paramId, currentValue)) {
        TrackedChanges.put(paramId, param);
        logger.debug("Parameter изменен для сечения '" + unitId + "': " +
          param.getName() + " = " + currentValue);
      }
    }
  }

  /**
   * Накапливает изменения топологий для конкретного юнита
   */
  private void trackTopologyChanges(String unitId, StoreData result) {
    Map<String, Topology> TrackedTopologyChanges = unitTrackedTopologyChanges.get(unitId);
    UnitDto unitDto = findUnitDtoById(result, unitId);
    if (unitDto == null) return;

    for (Topology topology : unitDto.getTopologyList()) {
      String topologyId = topology.getId();
      double currentValue = topology.getValue();

      if (isTopologyChanged(unitId, topologyId, currentValue)) {
        TrackedTopologyChanges.put(topologyId, topology);
        logger.debug("Topology изменен для сечения '" + unitId + "': " +
          topology.getName() + " = " + currentValue);
      }
    }
  }

  /**
   * Накапливает изменения элементов для конкретного юнита
   */
  private void trackElementChanges(String unitId, StoreData result) {
    Map<String, Element> TrackedElementChanges = unitTrackedElementChanges.get(unitId);
    UnitDto unitDto = findUnitDtoById(result, unitId);
    if (unitDto == null) return;

    for (Element element : unitDto.getElements()) {
      String elementId = element.getId();
      double currentValue = element.getValue();

      if (isElementChanged(unitId, elementId, currentValue)) {
        TrackedElementChanges.put(elementId, element);
        logger.debug("Element изменен для сечения '" + unitId + "': " +
          element.getName() + " = " + currentValue);
      }
    }
  }

  /**
   * Накапливает изменения элементов для конкретного юнита
   */
  private void trackInfluencingFactorChanges(String unitId, StoreData result) {
    Map<String, InfluencingFactor> TrackedInfluencingFactorChanges = unitTrackedFactorChanges.get(unitId);
    UnitDto unitDto = findUnitDtoById(result, unitId);
    if (unitDto == null) return;

    for (InfluencingFactor influencingFactor : unitDto.getInfluencingFactors()) {
      String influencingFactorId = influencingFactor.getId();
      double currentValue = influencingFactor.getValue();

      if (isFactorChanged(unitId, influencingFactorId, currentValue)) {
        TrackedInfluencingFactorChanges.put(influencingFactorId, influencingFactor);
        logger.debug("InfluencingFactor изменен для сечения '" + unitId + "': " +
          influencingFactor.getName() + " = " + currentValue);
      }
    }
  }

  /**
   * Создает StoreData из накопленных изменений, когда все условия выполнены:
   * все targetUids получены, одинаковый timestamp, initialDataLoaded, hasConsistentTimestamp
   */
  private StoreData createStoreDataFromTrackedChanges(String unitId) {
    StoreData TrackedResult = new StoreData();
    Map<String, Parameter> TrackedChanges = unitTrackedParameterChanges.get(unitId);
    Map<String, Topology> TrackedTopologyChanges = unitTrackedTopologyChanges.get(unitId);
    Map<String, Element> TrackedElementChanges = unitTrackedElementChanges.get(unitId);
    Map<String, InfluencingFactor> TrackedInfluencingFactorChanges = unitTrackedFactorChanges.get(unitId);

    if (!TrackedChanges.isEmpty() ||
      !TrackedTopologyChanges.isEmpty() ||
      !TrackedElementChanges.isEmpty() ||
      !TrackedInfluencingFactorChanges.isEmpty()) {
      Unit unit = findUnitByName(unitId);
      if (unit != null) {
        Map<String, Parameter> parameterChangesForUnit = new HashMap<>();
        Map<String, Topology> topologyChangesForUnit = new HashMap<>();
        Map<String, Element> elementChangesForUnit = new HashMap<>();
        Map<String, InfluencingFactor> influencingFactorChangesForUnit = new HashMap<>();

        for (Parameter param : TrackedChanges.values()) {
          if (isParameterBelongsToUnit(unit, param)) {
            parameterChangesForUnit.put(getMappedParameterKey(param), param);
          }
        }

        for (Topology topology : TrackedTopologyChanges.values()) {
          if (isTopologyBelongsToUnit(unit, topology)) {
            topologyChangesForUnit.put(topology.getId(), topology);
          }
        }

        for (Element element : TrackedElementChanges.values()) {
          if (isElementBelongsToUnit(unit, element)) {
            elementChangesForUnit.put(element.getId(), element);
          }
        }

        for (InfluencingFactor influencingFactor : TrackedInfluencingFactorChanges.values()) {
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
          TrackedResult.addUnitData(unitDto);

          logger.debug("Создан StoreData для сечения '" + unitId + "' с количеством изменений: " +
            " parameters=" + parameterChangesForUnit.size() +
            ", elements=" + elementChangesForUnit.size() +
            ", topologies=" + topologyChangesForUnit.size() +
            ", influencingFactor=" + influencingFactorChangesForUnit.size());
        }
      }
    }

    return TrackedResult;
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
  private void saveCurrentParameterValuesFromTracked(String unitId) {
    Map<String, Parameter> TrackedChanges = unitTrackedParameterChanges.get(unitId);
    Map<String, Double> previousParameterValues = unitPreviousParameterValues.get(unitId);
    for (Parameter param : TrackedChanges.values()) {
      previousParameterValues.put(param.getId(), param.getValue());
      logger.info("Предыдущие значения parameter обновлены для сечения '" + unitId +
        "': " + param.getName() + "/" + param.getId() + " = " + param.getValue() +
        ", timestamp=" + param.getTime());
    }
  }

  /**
   * Сохраняет текущие значения топологий из накопленных изменений
   */
  private void saveCurrentTopologyValuesFromTracked(String unitId) {
    Map<String, Topology> TrackedTopologyChanges = unitTrackedTopologyChanges.get(unitId);
    Map<String, Double> previousTopologyValues = unitPreviousTopologyValues.get(unitId);

    for (Topology topology : TrackedTopologyChanges.values()) {
      previousTopologyValues.put(topology.getId(), topology.getValue());
      logger.debug("Предыдущие значения topology обновлены для сечения '" + unitId +
        "': " + topology.getName() + " = " + topology.getValue());
    }
  }

  /**
   * Сохраняет текущие значения элементов из накопленных изменений
   */
  private void saveCurrentElementValuesFromTracked(String unitId) {
    Map<String, Element> TrackedElementChanges = unitTrackedElementChanges.get(unitId);
    Map<String, Double> previousElementValues = unitPreviousElementValues.get(unitId);

    for (Element element : TrackedElementChanges.values()) {
      previousElementValues.put(element.getId(), element.getValue());
      logger.debug("Предыдущие значения element обновлены для сечения '" + unitId +
        "': " + element.getName() + " = " + element.getValue());
    }
  }

  /**
   * Сохраняет текущие значения InfluencingFactor из накопленных изменений
   */
  private void saveCurrentInfluencingFactorValuesFromTracked(String unitId) {
    Map<String, InfluencingFactor> TrackedInfluencingFactorChanges = unitTrackedFactorChanges.get(unitId);
    Map<String, Double> previousInfluencingFactorValues = unitPreviousFactorValues.get(unitId);

    for (InfluencingFactor influencingFactor : TrackedInfluencingFactorChanges.values()) {
      previousInfluencingFactorValues.put(influencingFactor.getId(), influencingFactor.getValue());
      logger.debug("Предыдущие значения influencingFactor обновлены для сечения '" + unitId +
        "': " + influencingFactor.getName() + " = " + influencingFactor.getValue());
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
        logger.debug("Сохранено начальное значение parameter для сечения '" + unitId +
          "': " + param.getName() + " = " + currentValue);
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
        logger.debug("Сохранено начальное значение topology для сечения '" + unitId +
          "': " + topology.getName() + " = " + currentValue);
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
        logger.debug("Сохранено начальное значение element для сечения '" + unitId +
          "': " + element.getName() + " = " + currentValue);
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
        logger.debug("Сохранено начальное значение influencingFactor для сечения '" + unitId +
          "': " + influencingFactor.getName() + " = " + currentValue);
      }
    }
  }

  /**
   * Проверяет, изменилось ли значение параметра.
   * Сравнивает текущее значение с предыдущим сохраненным значением
   */
  private boolean isParameterChanged(String unitId, String paramId, double currentValue) {
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
  private boolean isTopologyChanged(String unitId, String topologyId, double currentValue) {
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
  private boolean isElementChanged(String unitId, String elementId, double currentValue) {
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
  private boolean isFactorChanged(String unitId, String elementId, double currentValue) {
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
