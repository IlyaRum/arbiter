package arbiter.measurement;

import arbiter.constants.ParameterMappingConstants;
import arbiter.constants.ParameterUnitResultMappingConstants;
import arbiter.data.*;
import arbiter.data.dto.FilteredUnitDto;
import arbiter.data.dto.UnitDto;
import arbiter.data.model.*;
import arbiter.di.DependencyInjector;
import arbiter.measurement.state.UnitState;
import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * Отслеживает изменения значений измерений (parameters, topology, elements, factors) для каждого сечения
 */

public class MeasurementChangeTracker {

  private static final Logger logger = LoggerFactory.getLogger(MeasurementChangeTracker.class);

  private final DependencyInjector dependencyInjector;
  private final DataReadyCallback dataReadyCallback;
  private final ExecutorService singleThreadExecutor;

  private final Map<String, Boolean> unitInitialDataLoaded = new ConcurrentHashMap<>();

  private final Map<String, Map<String, Double>> unitPreviousParameterValues = new ConcurrentHashMap<>();
  private final Map<String, Map<String, Double>> unitPreviousTopologyValues = new ConcurrentHashMap<>();
  private final Map<String, Map<String, Double>> unitPreviousElementValues = new ConcurrentHashMap<>();
  private final Map<String, Map<String, Double>> unitPreviousFactorValues = new ConcurrentHashMap<>();
  private final Map<String, Map<String, Double>> unitPreviousRepairValues = new ConcurrentHashMap<>();
  private final Map<String, Map<String, Double>> unitPreviousUnitResultValues = new ConcurrentHashMap<>();

  private final Map<String, Map<String, Parameter>> unitTrackedParameterChanges = new ConcurrentHashMap<>();
  private final Map<String, Map<String, Topology>> unitTrackedTopologyChanges = new ConcurrentHashMap<>();
  private final Map<String, Map<String, Element>> unitTrackedElementChanges = new ConcurrentHashMap<>();
  private final Map<String, Map<String, InfluencingFactor>> unitTrackedFactorChanges = new ConcurrentHashMap<>();
  private final Map<String, Map<String, Composition>> unitTrackedRepairChanges = new ConcurrentHashMap<>();
  private final Map<String, Map<String, UnitResult>> unitTrackedUnitResultChanges = new ConcurrentHashMap<>();


  public MeasurementChangeTracker(DependencyInjector dependencyInjector,
                                  DataReadyCallback dataReadyCallback,
                                  ExecutorService singleThreadExecutor) {
    this.dependencyInjector = dependencyInjector;
    this.dataReadyCallback = dataReadyCallback;
    this.singleThreadExecutor = singleThreadExecutor;
  }

  /**
   * Метод обрабатывает измерения и отслеживает изменения
   */
  public void processAndTrackChanges(MeasurementList list) {
    logger.debug("Получено новых измерений: " + list.size());

    StoreData result = new StoreData();
    List<Measurement> measurements = list.getMeasurements();

    for (int i = 0; i < measurements.size(); i++) {
      Measurement measurement = measurements.get(i);
      MemoryData memoryData = createMemoryData(measurement);

      List<Unit> units = dependencyInjector.getUnitCollection().getUnits();
      for (Unit unit : units) {
        processAndTrackForUnit(result, unit, memoryData);
      }
    }

    if (result.size() > 0) {
      trackChangesForAllUnits(result);
    }
  }

  private MemoryData createMemoryData(Measurement measurement) {
    String id = measurement.getUid();
    double value = measurement.getValue();
    Instant time = Instant.parse(measurement.getTimeStamp());
    int qCode = measurement.getQCode();

    return new MemoryData(id, value, time, qCode);
  }

  /**
   * Метод обработки и отслеживания изменений для одного юнита
   */
  private void processAndTrackForUnit(StoreData result, Unit unit, MemoryData memoryData) {
    String unitId = unit.getName();

    initializeUnitStateStructures(unitId);

    processAndTrackParameters(result, unit, unitId, memoryData);
    processAndTrackTopologies(result, unit, unitId, memoryData);
    processAndTrackElements(result, unit, unitId, memoryData);
    processAndTrackInfluencingFactors(result, unit, unitId, memoryData);
    processAndTrackRepairSchema(result, unit, unitId, memoryData);
    processAndTrackUnitResult(result, unit, unitId, memoryData);
  }

  /**
   * Объединенная логика обработки и отслеживания параметров
   */
  private void processAndTrackParameters(StoreData result, Unit unit, String unitId, MemoryData memoryData) {
    List<Parameter> parameters = unit.getParameters();

    for (Parameter parameter : parameters) {
      if (parameter.getId().equalsIgnoreCase(memoryData.getId())) {
        boolean isDataDifferent = parameter.isDataDifferent(memoryData.getValue(), memoryData.getTime());

        if (isDataDifferent) {
          parameter.setData(memoryData.getValue(), memoryData.getTime(), memoryData.getQCode());

          UnitDto unitDto = result.getUnitData(unit);
          if (unitDto == null) {
            unitDto = new UnitDto(unit);
            result.addUnitData(unitDto);
          }

          boolean initialDataLoaded = unitInitialDataLoaded.get(unitId);
          if (initialDataLoaded) {
            double currentValue = parameter.getValue();
            String paramId = parameter.getId();

            if (isParameterChanged(unitId, paramId, currentValue)) {
              Map<String, Parameter> trackedChanges = unitTrackedParameterChanges.get(unitId);
              trackedChanges.put(paramId, parameter);
              logger.debug("Parameter изменен для сечения '" + unitId + "': " +
                parameter.getName() + " = " + currentValue);
            }
          }
        }
        return;
      }
    }
  }

  /**
   * Объединенная логика обработки и отслеживания топологий
   */
  private void processAndTrackTopologies(StoreData result, Unit unit, String unitId, MemoryData memoryData) {
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

          boolean initialDataLoaded = unitInitialDataLoaded.get(unitId);
          if (initialDataLoaded) {
            double currentValue = topology.getValue();
            String topologyId = topology.getId();

            if (isTopologyChanged(unitId, topologyId, currentValue)) {
              Map<String, Topology> trackedChanges = unitTrackedTopologyChanges.get(unitId);
              trackedChanges.put(topologyId, topology);
              logger.debug("Topology изменен для сечения '" + unitId + "': " +
                topology.getName() + " = " + currentValue);
            }
          }
        }
        return;
      }
    }
  }

  /**
   * Объединенная логика обработки и отслеживания элементов
   */
  private void processAndTrackElements(StoreData result, Unit unit, String unitId, MemoryData memoryData) {
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

          boolean initialDataLoaded = unitInitialDataLoaded.get(unitId);
          if (initialDataLoaded) {
            double currentValue = element.getValue();
            String elementId = element.getId();

            if (isElementChanged(unitId, elementId, currentValue)) {
              Map<String, Element> trackedChanges = unitTrackedElementChanges.get(unitId);
              trackedChanges.put(elementId, element);
              logger.debug("Element изменен для сечения '" + unitId + "': " +
                element.getName() + " = " + currentValue);
            }
          }
        }
        return;
      }
    }
  }

  private void processAndTrackUnitResult(StoreData result, Unit unit, String unitId, MemoryData memoryData) {
    List<UnitResult> unitResults = unit.getUnitResults();

    for (UnitResult unitResult : unitResults) {
      if (unitResult.getUid().equalsIgnoreCase(memoryData.getId())) {
        boolean isDataDifferent = unitResult.isDataDifferent(memoryData.getValue(), memoryData.getTime());

        if (isDataDifferent) {
          unitResult.setData(memoryData.getValue(), memoryData.getTime(), memoryData.getQCode());

          UnitDto unitDto = result.getUnitData(unit);
          if (unitDto == null) {
            unitDto = new UnitDto(unit);
            result.addUnitData(unitDto);
          }

          boolean initialDataLoaded = unitInitialDataLoaded.get(unitId);
          if (initialDataLoaded) {
            double currentValue = unitResult.getValue();
            String paramId = unitResult.getUid();

            if (isUnitResultChanged(unitId, paramId, currentValue)) {
              Map<String, UnitResult> trackedChanges = unitTrackedUnitResultChanges.get(unitId);
              trackedChanges.put(paramId, unitResult);
              logger.debug("UnitResult изменен для сечения '" + unitId + "': " +
                unitResult.getName() + " = " + currentValue);
            }
          }
        }
        return;
      }
    }
  }

  /**
   * Объединенная логика обработки и отслеживания влияющих факторов
   */
  private void processAndTrackInfluencingFactors(StoreData result, Unit unit, String unitId, MemoryData memoryData) {
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

          boolean initialDataLoaded = unitInitialDataLoaded.get(unitId);
          if (initialDataLoaded) {
            double currentValue = influencingFactor.getValue();
            String influencingFactorId = influencingFactor.getId();

            if (isFactorChanged(unitId, influencingFactorId, currentValue)) {
              Map<String, InfluencingFactor> trackedChanges = unitTrackedFactorChanges.get(unitId);
              trackedChanges.put(influencingFactorId, influencingFactor);
              logger.debug("InfluencingFactor изменен для сечения '" + unitId + "': " +
                influencingFactor.getName() + " = " + currentValue);
            }
          }
        }
        return;
      }
    }
  }

  /**
   * Объединенная логика обработки и отслеживания ремонтных схем
   */
  private void processAndTrackRepairSchema(StoreData result, Unit unit, String unitId, MemoryData memoryData) {
    RepairSchema repairSchema = unit.getRepairSchema();
    if (repairSchema == null) return;

    List<RepairGroupValue> repairGroupValues = repairSchema.getRepairGroupValues();
    if (repairGroupValues == null || repairGroupValues.isEmpty()) return;

    for (RepairGroupValue repairGroup : repairGroupValues) {
      List<Composition> compositions = repairGroup.getValues();
      if (compositions == null) continue;

      for (Composition composition : compositions) {
        if (composition.getId().equalsIgnoreCase(memoryData.getId())) {
          if (composition.isDataDifferent(memoryData.getValue(), memoryData.getTime())) {
            composition.setData(memoryData.getValue(), memoryData.getTime(), memoryData.getQCode());

            UnitDto unitDto = result.getUnitData(unit);
            if (unitDto == null) {
              unitDto = new UnitDto(unit);
              result.addUnitData(unitDto);
            }

            boolean initialDataLoaded = unitInitialDataLoaded.get(unitId);
            if (initialDataLoaded) {
              double currentValue = composition.getValue();
              String compositionId = composition.getId();

              if (isRepairChanged(unitId, compositionId, currentValue)) {
                Map<String, Composition> trackedChanges = unitTrackedRepairChanges.get(unitId);
                trackedChanges.put(compositionId, composition);
                logger.debug("Repair (Composition) изменен для сечения '" + unitId + "': " +
                  composition.getName() + " = " + currentValue);
              }
            }
          }
          return;
        }
      }
    }
  }

  private void sendTrackedChangesIfNeeded(String unitId, UnitState unitState) {
    int paramChanges = unitState.getTrackedParameterChanges().size();
    int topologyChanges = unitState.getTrackedTopologyChanges().size();
    int elementChanges = unitState.getTrackedElementChanges().size();
    int factorChanges = unitState.getTrackedInfluencingFactorChanges().size();
    int repairChanges = unitState.getTrackedRepairChanges().size();
    int unitResult = unitState.getTrackedUnitResultChanges().size();

    if (paramChanges > 0 || topologyChanges > 0 || elementChanges > 0 ||
      factorChanges > 0 || repairChanges > 0 || unitResult > 0) {

      logger.info(String.format("Новые изменения для сечения '%s': Parameter=%d, Topology=%d, Element=%d, Factor=%d, Repair=%d, unitResult=%d",
        unitId, paramChanges, topologyChanges, elementChanges, factorChanges, repairChanges, unitResult));

      sendTrackedChanges(unitId, unitState);
    } else {
      logger.info("Нет изменений для отправки");
    }
  }

  private void trackChangesForAllUnits(StoreData result) {
    int unitProcessedCount = 0;
    for (UnitDto unitDto : result.getUnitDataList()) {
      unitProcessedCount++;
      trackChangesForUnit(result, unitDto, unitProcessedCount);
    }
  }

  private void trackChangesForUnit(StoreData result, UnitDto unitDto, int unitProcessedCount) {
    String unitId = getUnitIdentifier(unitDto);
    int totalCount = result.getUnitDataList().size();
    logger.info(String.format("[%d/%d] Обработка сечения: %s", unitProcessedCount, totalCount, unitId));

    trackedChanges(result, unitId);
  }

  private UnitState getUnitState(String unitId) {
    return new UnitState(
      unitTrackedParameterChanges.get(unitId),
      unitTrackedTopologyChanges.get(unitId),
      unitTrackedElementChanges.get(unitId),
      unitTrackedFactorChanges.get(unitId),
      unitTrackedRepairChanges.get(unitId),
      unitTrackedUnitResultChanges.get(unitId)
    );
  }

  private void trackedChanges(StoreData result, String unitId) {
    boolean initialDataLoaded = unitInitialDataLoaded.get(unitId);
    UnitState unitState = getUnitState(unitId);

    if (!initialDataLoaded) {
      loadInitialValues(result, unitId);
    } else {
      sendTrackedChangesIfNeeded(unitId, unitState);
    }
  }

  private void loadInitialValues(StoreData result, String unitId) {
    logger.debug("Первичная загрузка данных для сечения '" + unitId + "'");
    unitInitialDataLoaded.put(unitId, true);
    saveCurrentParameterValues(unitId, result);
    saveCurrentTopologyValues(unitId, result);
    saveCurrentElementValues(unitId, result);
    saveCurrentInfluencingFactorValues(unitId, result);
    saveCurrentRepairValues(unitId, result);
    saveCurrentUnitResultValues(unitId, result);
    logger.debug("Начальные данные загружены для сечения '" + unitId + "'");
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
    unitState.getTrackedParameterChanges().clear();
    unitState.getTrackedTopologyChanges().clear();
    unitState.getTrackedElementChanges().clear();
    unitState.getTrackedInfluencingFactorChanges().clear();
    unitState.getTrackedRepairChanges().clear();
    unitState.getTrackedUnitResultChanges().clear();
    logger.info("Накопленные изменения очищены \n");
  }

  private void saveCurrentValuesFromTracked(String unitId) {
    saveCurrentParameterValuesFromTracked(unitId);
    saveCurrentTopologyValuesFromTracked(unitId);
    saveCurrentElementValuesFromTracked(unitId);
    saveCurrentInfluencingFactorValuesFromTracked(unitId);
    saveCurrentRepairValuesFromTracked(unitId);
    saveCurrentUnitResultValuesFromTracked(unitId);
    logger.info("Текущие значения сохранены как предыдущие");
  }

  private void initializeUnitStateStructures(String unitId) {
    unitInitialDataLoaded.putIfAbsent(unitId, false);

    unitPreviousParameterValues.computeIfAbsent(unitId, k -> new ConcurrentHashMap<>());
    unitPreviousTopologyValues.computeIfAbsent(unitId, k -> new ConcurrentHashMap<>());
    unitPreviousElementValues.computeIfAbsent(unitId, k -> new ConcurrentHashMap<>());
    unitPreviousFactorValues.computeIfAbsent(unitId, k -> new ConcurrentHashMap<>());
    unitPreviousRepairValues.computeIfAbsent(unitId, k-> new ConcurrentHashMap<>());
    unitPreviousUnitResultValues.computeIfAbsent(unitId, k-> new ConcurrentHashMap<>());

    unitTrackedParameterChanges.computeIfAbsent(unitId, k -> new ConcurrentHashMap<>());
    unitTrackedTopologyChanges.computeIfAbsent(unitId, k -> new ConcurrentHashMap<>());
    unitTrackedElementChanges.computeIfAbsent(unitId, k -> new ConcurrentHashMap<>());
    unitTrackedFactorChanges.computeIfAbsent(unitId, k -> new ConcurrentHashMap<>());
    unitTrackedRepairChanges.computeIfAbsent(unitId, k -> new ConcurrentHashMap<>());
    unitTrackedUnitResultChanges.computeIfAbsent(unitId, k -> new ConcurrentHashMap<>());
    logger.debug("Структуры состояния инициализированы для сечения: " + unitId);
  }

  /**
   * Создает StoreData, состоящий только из новых изменений для конкретного сечения:
   */
  private StoreData createStoreDataFromTrackedChanges(String unitId) {
    StoreData trackedResult = new StoreData();
    Map<String, Parameter> trackedParameterChanges = unitTrackedParameterChanges.get(unitId);
    Map<String, Topology> trackedTopologyChanges = unitTrackedTopologyChanges.get(unitId);
    Map<String, Element> trackedElementChanges = unitTrackedElementChanges.get(unitId);
    Map<String, InfluencingFactor> trackedInfluencingFactorChanges = unitTrackedFactorChanges.get(unitId);
    Map<String, Composition> trackedRepairChanges = unitTrackedRepairChanges.get(unitId);
    Map<String, UnitResult> trackedUnitResultChanges = unitTrackedUnitResultChanges.get(unitId);

    if (!trackedParameterChanges.isEmpty() ||
      !trackedTopologyChanges.isEmpty() ||
      !trackedElementChanges.isEmpty() ||
      !trackedInfluencingFactorChanges.isEmpty() ||
      !trackedRepairChanges.isEmpty() ||
      !trackedUnitResultChanges.isEmpty()) {
      Unit unit = findUnitByName(unitId);
      if (unit != null) {

        Map<String, Parameter> parameterChangesForUnit = new HashMap<>();
        Map<String, Topology> topologyChangesForUnit = new HashMap<>();
        Map<String, Element> elementChangesForUnit = new HashMap<>();
        Map<String, InfluencingFactor> influencingFactorChangesForUnit = new HashMap<>();
        Map<String, Composition> repairChangesForUnit = new HashMap<>();
        Map<String, UnitResult> unitResultChangesForUnit = new HashMap<>();

        for (Parameter param : trackedParameterChanges.values()) {
          if (isParameterBelongsToUnit(unit, param)) {
            parameterChangesForUnit.put(getMappedParameterKey(param), param);
          }
        }

        for (Topology topology : trackedTopologyChanges.values()) {
          if (isTopologyBelongsToUnit(unit, topology)) {
            topologyChangesForUnit.put(topology.getId(), topology);
          }
        }

        for (Element element : trackedElementChanges.values()) {
          if (isElementBelongsToUnit(unit, element)) {
            elementChangesForUnit.put(element.getId(), element);
          }
        }

        for (InfluencingFactor influencingFactor : trackedInfluencingFactorChanges.values()) {
          if (isInfluencingFactorBelongsToUnit(unit, influencingFactor)) {
            influencingFactorChangesForUnit.put(influencingFactor.getId(), influencingFactor);
          }
        }

        for (Composition composition : trackedRepairChanges.values()) {
          if (isCompositionBelongsToUnitRepairSchema(unit, composition)) {
            repairChangesForUnit.put(composition.getId(), composition);
          }
        }

        for (UnitResult unitResult : trackedUnitResultChanges.values()) {
          if (isUnitResultBelongsToUnit(unit, unitResult)) {
            unitResultChangesForUnit.put(getMappedUnitResultKey(unitResult), unitResult);
          }
        }

        if (!parameterChangesForUnit.isEmpty() ||
          !topologyChangesForUnit.isEmpty() ||
          !elementChangesForUnit.isEmpty() ||
          !influencingFactorChangesForUnit.isEmpty() ||
          !repairChangesForUnit.isEmpty() ||
          !unitResultChangesForUnit.isEmpty()) {

          RepairSchema filteredRepairSchema = null;
          if (!repairChangesForUnit.isEmpty()) {
            filteredRepairSchema = createFilteredRepairSchema(unit.getRepairSchema(), repairChangesForUnit);
          }

          FilteredUnitDto unitDto = new FilteredUnitDto(new UnitDto(unit),
            parameterChangesForUnit,
            topologyChangesForUnit,
            elementChangesForUnit,
            influencingFactorChangesForUnit,
            filteredRepairSchema,
            unitResultChangesForUnit);

          trackedResult.addUnitData(unitDto);

          logger.debug("Создан StoreData для сечения '" + unitId + "' с количеством изменений: " +
            " parameters=" + parameterChangesForUnit.size() +
            ", elements=" + elementChangesForUnit.size() +
            ", topologies=" + topologyChangesForUnit.size() +
            ", influencingFactor=" + influencingFactorChangesForUnit.size() +
            ", repairs=" + repairChangesForUnit.size() +
            ", unitResult=" + unitResultChangesForUnit.size());
        }
      }
    }

    return trackedResult;
  }

  /**
   * Создает отфильтрованную ремонтную схему с только изменившимися значениями Composition
   */
  private RepairSchema createFilteredRepairSchema(RepairSchema originalRepairSchema, Map<String, Composition> changedCompositions) {
    if (originalRepairSchema == null || changedCompositions.isEmpty()) {
      return null;
    }

    RepairSchema filteredRepairSchema = new RepairSchema();
    filteredRepairSchema.setCheckFormula(originalRepairSchema.getCheckFormula());

    List<RepairGroupValue> filteredRepairGroupValues = new ArrayList<>();

    if (originalRepairSchema.getRepairGroupValues() != null) {
      for (RepairGroupValue originalGroup : originalRepairSchema.getRepairGroupValues()) {
        RepairGroupValue filteredGroup = new RepairGroupValue();
        filteredGroup.setGroup(originalGroup.getGroup());
        filteredGroup.setOperation(originalGroup.getOperation());

        List<Composition> filteredCompositions = new ArrayList<>();

        if (originalGroup.getValues() != null) {
          for (Composition composition : originalGroup.getValues()) {
            if (changedCompositions.containsKey(composition.getId())) {
              filteredCompositions.add(changedCompositions.get(composition.getId()));
            }
          }
        }

        if (!filteredCompositions.isEmpty()) {
          filteredGroup.setValues(filteredCompositions);
          filteredRepairGroupValues.add(filteredGroup);
        }
      }
    }

    filteredRepairSchema.setRepairGroupValues(filteredRepairGroupValues);
    return filteredRepairGroupValues.isEmpty() ? null : filteredRepairSchema;
  }

  /**
   * Проверяет принадлежность Composition к ремонтной схеме сечения
   */
  private boolean isCompositionBelongsToUnitRepairSchema(Unit unit, Composition composition) {
    RepairSchema repairSchema = unit.getRepairSchema();
    if (repairSchema == null || repairSchema.getRepairGroupValues() == null) {
      return false;
    }

    for (RepairGroupValue repairGroup : repairSchema.getRepairGroupValues()) {
      if (repairGroup.getValues() != null) {
        for (Composition unitComposition : repairGroup.getValues()) {
          if (unitComposition.getId().equals(composition.getId())) {
            return true;
          }
        }
      }
    }

    return false;
  }

  private boolean isUnitResultBelongsToUnit(Unit unit, UnitResult result) {
    for (UnitResult unitResult : unit.getUnitResults()) {
      if (unitResult.getUid().equals(result.getUid())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Находит сечение по имени
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
   * Сохраняем текущие значения как предыдущие для следующего сравнения для конкретного юнита
   */
  private void saveCurrentUnitResultValuesFromTracked(String unitId) {
    Map<String, UnitResult> trackedChanges = unitTrackedUnitResultChanges.get(unitId);
    Map<String, Double> previousUnitResultValues = unitPreviousUnitResultValues.get(unitId);
    for (UnitResult result : trackedChanges.values()) {
      previousUnitResultValues.put(result.getUid(), result.getValue());
      logger.info("Предыдущие значения unitResult обновлены для сечения '" + unitId +
        "': " + result.getName() + "/" + result.getUid() + " = " + result.getValue() +
        ", timestamp=" + result.getTime());
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

  private void saveCurrentRepairValuesFromTracked(String unitId) {
    Map<String, Composition> trackedRepairChanges = unitTrackedRepairChanges.get(unitId);
    Map<String, Double> previousRepairValues = unitPreviousRepairValues.get(unitId);

    for (Composition composition : trackedRepairChanges.values()) {
      previousRepairValues.put(composition.getId(), composition.getValue());
      logger.debug("Предыдущие значения repair обновлены для сечения '" + unitId +
        "': " + composition.getName() + " = " + composition.getValue());
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

  private String getMappedUnitResultKey(UnitResult unitResult) {
    //TODO [IER] Используем ту же логику, что и в UnitDto
    // Нужно отрефакторить, чтобы использовать один метод
    return ParameterUnitResultMappingConstants.PARAMETER_RESULT_NAME_TO_FIELD_MAPPING.getOrDefault(unitResult.getName(), unitResult.getUid());
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
   * Сохраняет текущие значения параметров для конкретного юнита
   */
  private void saveCurrentUnitResultValues(String unitId, StoreData result) {
    Map<String, Double> previousUnitResultValues = unitPreviousUnitResultValues.get(unitId);
    UnitDto unitDto = findUnitDtoById(result, unitId);
    if (unitDto != null) {
      for (UnitResult unitResult : unitDto.getResult().values()) {
        String id = unitResult.getUid();
        double currentValue = unitResult.getValue();
        previousUnitResultValues.put(id, currentValue);
        logger.debug("Сохранено начальное значение unitResult для сечения '" + unitId +
          "': " + unitResult.getName() + " = " + currentValue);
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

  private void saveCurrentRepairValues(String unitId, StoreData result) {
    Map<String, Double> previousRepairValues = unitPreviousRepairValues.get(unitId);
    UnitDto unitDto = findUnitDtoById(result, unitId);
    if (unitDto != null) {
      RepairSchema repairSchema = unitDto.getRepairSchema();
      if (repairSchema != null && repairSchema.getRepairGroupValues() != null) {
        for (RepairGroupValue repairGroup : repairSchema.getRepairGroupValues()) {
          if (repairGroup.getValues() != null) {
            for (Composition composition : repairGroup.getValues()) {
              double currentValue = composition.getValue();
              previousRepairValues.put(composition.getId(), currentValue);
              logger.debug("Сохранено начальное значение repair для сечения '" + unitId +
                "': " + composition.getName() + " = " + currentValue);
            }
          }
        }
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

    if (previousValue == null) {
      return true;
    }

    return Math.abs(currentValue - previousValue) > 1e-10;
  }

  private boolean isUnitResultChanged(String unitId, String paramId, double currentValue) {
    Map<String, Double> previousUnitResultValues = unitPreviousUnitResultValues.get(unitId);
    Double previousValue = previousUnitResultValues.get(paramId);
    if (previousValue == null) {
      return true;
    }

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
   * Проверяет, изменилось ли значение ремонтной схемы (Composition)
   */
  private boolean isRepairChanged(String unitId, String compositionId, double currentValue) {
    Map<String, Double> previousRepairValues = unitPreviousRepairValues.get(unitId);
    if (previousRepairValues == null) {
      return true;
    }

    Double previousValue = previousRepairValues.get(compositionId);
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
