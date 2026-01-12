package arbiter.measurement;

import arbiter.config.TestUnitCollection;
import arbiter.data.*;
import arbiter.data.dto.UnitDto;
import arbiter.data.model.*;
import arbiter.di.DependencyInjector;
import arbiter.helper.MeasurementDataReflectionTestHelper;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutorService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MeasurementDataProcessor
 */
@ExtendWith(MockitoExtension.class)
class MeasurementDataProcessorTest {

  @Mock
  private DependencyInjector dependencyInjector;

  @Mock
  private DataReadyCallback dataReadyCallback;

  @Mock
  private ExecutorService singleThreadExecutor;

  @Mock
  private UnitCollection mockUnitCollection;

  private Vertx vertx;
  private MeasurementDataProcessor processor;

  @BeforeEach
  void setUp() {
    processor = new MeasurementDataProcessor(dependencyInjector);
    processor.setDataReadyCallback(dataReadyCallback);

  }

  @Test
  void testOnDataReceived_EmptyMeasurementList_DoesNotCallCallback() {
    MeasurementList emptyList = new MeasurementList();

    processor.onDataReceived(emptyList);

    verify(dataReadyCallback, never()).onDataReady(any(StoreData.class), any());
  }

  @Test
  void testOnDataReceived_Exception_LogsError() {
    MeasurementList list = mock(MeasurementList.class);
    when(list.size()).thenThrow(new RuntimeException("Test exception"));

    assertDoesNotThrow(() -> processor.onDataReceived(list));
    verify(dataReadyCallback, never()).onDataReady(any(StoreData.class), any());
  }

  @Test
  void testOnDataReceived_WithMeasurement_ProcessesParameters() {
    String paramId = "df092d1f-b435-4873-85af-39fa675b611e";
    String paramName = "param name";
    double paramValue = 42.5;
    Instant paramTime = Instant.now();
    int qCode = 1879048194;

    Measurement measurement = createMeasurement(paramId, paramValue, paramTime, qCode);
    MeasurementList list = new MeasurementList();
    list.add(measurement);

    Unit unit = mock(Unit.class);
    when(unit.getName()).thenReturn("unit name");

    Parameter parameter = mock(Parameter.class);
    when(parameter.getId()).thenReturn(paramId);
    when(parameter.getName()).thenReturn(paramName);
    when(parameter.isDataDifferent(eq(paramValue), eq(paramTime))).thenReturn(true);

    List<Parameter> parameters = new ArrayList<>();
    parameters.add(parameter);

    when(unit.getParameters()).thenReturn(parameters);
    when(unit.getTopologies()).thenReturn(Collections.emptyList());
    when(unit.getElements()).thenReturn(Collections.emptyList());
    when(unit.getInfluencingFactors()).thenReturn(Collections.emptyList());
    when(unit.getRepairSchema()).thenReturn(null);

    List<Unit> units = new ArrayList<>();
    units.add(unit);

    //на моках test проходит
    when(mockUnitCollection.getUnits()).thenReturn(units);
    when(dependencyInjector.getUnitCollection()).thenReturn(mockUnitCollection);

    processor.onDataReceived(list);

//    assertEquals(paramValue, parameter.getValue(), 0.001);
//    assertEquals(paramTime, parameter.getTime());
//    assertEquals(qCode, parameter.getQCode());
    verify(parameter).setData(eq(paramValue), eq(paramTime), eq(qCode));
    verify(dataReadyCallback, timeout(1000)).onDataReady(any(StoreData.class), isNull());
  }

  @Test
  void testOnDataReceived_WithMeasurement_ProcessesTopologies() {
    // Arrange
    String topologyId = "test-topology-id";
    double topologyValue = 1.0;
    Instant topologyTime = Instant.now();
    int qCode = 1879048194;

    Measurement measurement = createMeasurement(topologyId, topologyValue, topologyTime, qCode);
    MeasurementList list = new MeasurementList();
    list.add(measurement);

    Unit unit = mock(Unit.class);
    when(unit.getName()).thenReturn("unit name");
    Topology topology = mock(Topology.class);

    when(topology.getId()).thenReturn(topologyId);
    when(topology.isDataDifferent(eq(topologyValue), eq(topologyTime))).thenReturn(true);

    List<Topology> topologies = new ArrayList<>();
    topologies.add(topology);

    when(unit.getParameters()).thenReturn(Collections.emptyList());
    when(unit.getTopologies()).thenReturn(topologies);
    when(unit.getElements()).thenReturn(Collections.emptyList());
    when(unit.getInfluencingFactors()).thenReturn(Collections.emptyList());
    when(unit.getRepairSchema()).thenReturn(null);

    List<Unit> units = new ArrayList<>();
    units.add(unit);

    when(mockUnitCollection.getUnits()).thenReturn(units);
    when(dependencyInjector.getUnitCollection()).thenReturn(mockUnitCollection);

//    when(dependencyInjector.getUnitCollection()).thenReturn(realUnitCollection);
//    when(realUnitCollection.getUnits()).thenReturn(units);

    // Act
    processor.onDataReceived(list);

    // Assert
    verify(topology).setData(eq(topologyValue), eq(topologyTime), eq(qCode));
  }

  @Test
  void testOnDataReceived_WithMeasurement_ProcessesElements() {
    // Arrange
    String elementId = "test-element-id";
    double elementValue = 10.5;
    Instant elementTime = Instant.now();
    int qCode = 1879048194;

    Measurement measurement = createMeasurement(elementId, elementValue, elementTime, qCode);
    MeasurementList list = new MeasurementList();
    list.add(measurement);

    // Create mock unit with element
    Unit unit = mock(Unit.class);
    when(unit.getName()).thenReturn("unit name");
    Element element = mock(Element.class);

    when(element.getId()).thenReturn(elementId);
    when(element.isDataDifferent(eq(elementValue), eq(elementTime))).thenReturn(true);

    List<Element> elements = new ArrayList<>();
    elements.add(element);

    when(unit.getParameters()).thenReturn(Collections.emptyList());
    when(unit.getTopologies()).thenReturn(Collections.emptyList());
    when(unit.getElements()).thenReturn(elements);
    when(unit.getInfluencingFactors()).thenReturn(Collections.emptyList());
    when(unit.getRepairSchema()).thenReturn(null);

    List<Unit> units = new ArrayList<>();
    units.add(unit);

    when(dependencyInjector.getUnitCollection()).thenReturn(mockUnitCollection);
    when(mockUnitCollection.getUnits()).thenReturn(units);

    // Act
    processor.onDataReceived(list);

    // Assert
    verify(element).setData(eq(elementValue), eq(elementTime), eq(qCode));
  }

  @Test
  void testOnDataReceived_WithMeasurement_ProcessesInfluencingFactors() {
    // Arrange
    String factorId = "test-factor-id";
    double factorValue = 5.5;
    Instant factorTime = Instant.now();
    int qCode = 1879048194;

    Measurement measurement = createMeasurement(factorId, factorValue, factorTime, qCode);
    MeasurementList list = new MeasurementList();
    list.add(measurement);

    // Create mock unit with influencing factor
//    Unit unit = createTestUnit();
    Unit unit = mock(Unit.class);
    when(unit.getName()).thenReturn("unit name");


//    InfluencingFactor factor = new InfluencingFactor(factorId, "Test Factor");
//    factor.setData(factorValue - 1.0, factorTime.minusSeconds(1), 0);

    InfluencingFactor factor = mock(InfluencingFactor.class);
    when(factor.getId()).thenReturn(factorId);
    when(factor.isDataDifferent(eq(factorValue), eq(factorTime))).thenReturn(true);

//    when(factor.getId()).thenReturn(factorId);
//    when(factor.isDataDifferent(eq(factorValue), eq(factorTime))).thenReturn(true);

    List<InfluencingFactor> factors = new ArrayList<>();
    factors.add(factor);

    //reflectionTestHelper.setField(unit, "influencingFactors", factors);

    when(unit.getParameters()).thenReturn(Collections.emptyList());
    when(unit.getTopologies()).thenReturn(Collections.emptyList());
    when(unit.getElements()).thenReturn(Collections.emptyList());
    when(unit.getInfluencingFactors()).thenReturn(factors);
    when(unit.getRepairSchema()).thenReturn(null);

    List<Unit> units = new ArrayList<Unit>();
    units.add(unit);

    when(dependencyInjector.getUnitCollection()).thenReturn(mockUnitCollection);
    when(mockUnitCollection.getUnits()).thenReturn(units);

    // Act
    processor.onDataReceived(list);

    // Assert
//    assertEquals(factorValue, factor.getValue(), 0.001);
//    assertEquals(factorTime, factor.getTime());
//    assertEquals(qCode, factor.getqCode());
    verify(factor).setData(eq(factorValue), eq(factorTime), eq(qCode));
  }

  @Test
  void testOnDataReceived_WithMeasurement_ProcessesRepairSchema() {
    // Arrange
    String compositionId = "test-composition-id";
    double compositionValue = 3.5;
    Instant compositionTime = Instant.now();
    int qCode = 1879048194;

    Measurement measurement = createMeasurement(compositionId, compositionValue, compositionTime, qCode);
    MeasurementList list = new MeasurementList();
    list.add(measurement);

    // Create mock unit with repair schema
    Unit unit = mock(Unit.class);
    when(unit.getName()).thenReturn("unit name");
    RepairSchema repairSchema = mock(RepairSchema.class);
    RepairGroupValue repairGroupValue = mock(RepairGroupValue.class);
    Composition composition = mock(Composition.class);

    when(composition.getId()).thenReturn(compositionId);
    when(composition.isDataDifferent(eq(compositionValue), eq(compositionTime))).thenReturn(true);

    List<Composition> compositions = new ArrayList<>();
    compositions.add(composition);

    when(repairGroupValue.getValues()).thenReturn(compositions);

    List<RepairGroupValue> repairGroupValues = new ArrayList<>();
    repairGroupValues.add(repairGroupValue);

    when(repairSchema.getRepairGroupValues()).thenReturn(repairGroupValues);

    when(unit.getParameters()).thenReturn(Collections.emptyList());
    when(unit.getTopologies()).thenReturn(Collections.emptyList());
    when(unit.getElements()).thenReturn(Collections.emptyList());
    when(unit.getInfluencingFactors()).thenReturn(Collections.emptyList());
    when(unit.getRepairSchema()).thenReturn(repairSchema);

    List<Unit> units = new ArrayList<>();
    units.add(unit);

    when(dependencyInjector.getUnitCollection()).thenReturn(mockUnitCollection);
    when(mockUnitCollection.getUnits()).thenReturn(units);

    // Act
    processor.onDataReceived(list);

    // Assert
    verify(composition).setData(eq(compositionValue), eq(compositionTime), eq(qCode));
  }

  @Test
  void testOnDataReceived_DataNotDifferent_DoesNotUpdate() {
    String paramId = "test-param-id";
    double paramValue = 42.5;
    Instant paramTime = Instant.now();
    int qCode = 1879048194;

    Measurement measurement = createMeasurement(paramId, paramValue, paramTime, qCode);
    MeasurementList list = new MeasurementList();
    list.add(measurement);

    Unit unit = mock(Unit.class);
    Parameter parameter = mock(Parameter.class);

    when(parameter.getId()).thenReturn(paramId);
    when(parameter.isDataDifferent(eq(paramValue), eq(paramTime))).thenReturn(false); // Data hasn't changed

    List<Parameter> parameters = new ArrayList<>();
    parameters.add(parameter);

    when(unit.getParameters()).thenReturn(parameters);
    when(unit.getTopologies()).thenReturn(Collections.emptyList());
    when(unit.getElements()).thenReturn(Collections.emptyList());
    when(unit.getInfluencingFactors()).thenReturn(Collections.emptyList());
    when(unit.getRepairSchema()).thenReturn(null);

    List<Unit> units = new ArrayList<>();
    units.add(unit);

    when(dependencyInjector.getUnitCollection()).thenReturn(mockUnitCollection);
    when(mockUnitCollection.getUnits()).thenReturn(units);

    processor.onDataReceived(list);

    verify(parameter, never()).setData(anyDouble(), any(Instant.class), anyInt());
  }

  @Test
  void testOnDataReceived_ExceptionInProcessing_LogsError() {
    MeasurementList list = new MeasurementList();
    list.add(createMeasurement("test-id", 1.0, Instant.now(), 1));

    when(dependencyInjector.getUnitCollection()).thenThrow(new RuntimeException("Test exception"));

    processor.onDataReceived(list);

    assertDoesNotThrow(() -> processor.onDataReceived(list));
  }

  @Test
  void testOnDataReceived_SecondTime_CreatesBatchAggregator() {
    MeasurementList list = createTestMeasurementList();

    when(dependencyInjector.getUnitCollection()).thenReturn(mockUnitCollection);
    when(mockUnitCollection.getUnits()).thenReturn(Collections.emptyList());

    processor.onDataReceived(list);

    reset(dataReadyCallback);
    processor.setDataReadyCallback(dataReadyCallback);

    MeasurementList secondList = new MeasurementList();
    secondList.add(createMeasurement("test-id-2", 2.0, Instant.now(), 1));

    processor.onDataReceived(secondList);

    verify(dataReadyCallback, never()).onDataReady(any(StoreData.class), eq(null));
  }

//  @Test
//  void testFindUnitForParameter_ReturnsCorrectUnit() {
//    // Arrange
//    String paramId = "test-param-id";
//
//    Unit unit1 = mock(Unit.class);
//    Unit unit2 = mock(Unit.class);
//
//    Parameter param1 = mock(Parameter.class);
//    Parameter param2 = mock(Parameter.class);
//
//    when(param1.getId()).thenReturn(paramId);
//    when(param2.getId()).thenReturn("other-param-id");
//
//    List<Parameter> params1 = Collections.singletonList(param1);
//    List<Parameter> params2 = Collections.singletonList(param2);
//
//    when(unit1.getParameters()).thenReturn(params1);
//    when(unit2.getParameters()).thenReturn(params2);
//
//    List<Unit> units = Arrays.asList(unit1, unit2);
//    when(dependencyInjector.getUnitCollection()).thenReturn(unitCollection);
//    when(unitCollection.getUnits()).thenReturn(units);
//
//    Parameter searchParam = mock(Parameter.class);
//    when(searchParam.getId()).thenReturn(paramId);
//
//    // Act
//    Unit result = processor.findUnitForParameter(searchParam);
//
//    // Assert
//    assertEquals(unit1, result);
//  }

//  @Test
//  void testFindUnitForParameter_NotFound_ReturnsNull() {
//    // Arrange
//    String paramId = "non-existent-param";
//
//    Unit unit = mock(Unit.class);
//    Parameter param = mock(Parameter.class);
//
//    when(param.getId()).thenReturn("different-param");
//    List<Parameter> params = Collections.singletonList(param);
//    when(unit.getParameters()).thenReturn(params);
//
//    List<Unit> units = Collections.singletonList(unit);
//    when(dependencyInjector.getUnitCollection()).thenReturn(unitCollection);
//    when(unitCollection.getUnits()).thenReturn(units);
//
//    Parameter searchParam = mock(Parameter.class);
//    when(searchParam.getId()).thenReturn(paramId);
//
//    // Act
//    Unit result = processor.findUnitForParameter(searchParam);
//
//    // Assert
//    assertNull(result);
//  }


//  @Test
//  void testCreateMemoryData_ValidMeasurement_CreatesCorrectMemoryData() {
//    // Arrange
//    String id = "test-id";
//    double value = 123.45;
//    Instant time = Instant.now();
//    int qCode = 1879048194;
//
//    Measurement measurement = createMeasurement(id, value, time, qCode);
//
//    // Act
//    MemoryData memoryData = processor.createMemoryData(measurement);
//
//    // Assert
//    assertEquals(id, memoryData.getId());
//    assertEquals(value, memoryData.getValue(), 0.001);
//    assertEquals(time, memoryData.getTime());
//    assertEquals(qCode, memoryData.getQCode());
//  }

//  @Test
//  void testProcessMeasurementsToStoreData_EmptyList_ReturnsEmptyStoreData() {
//    // Arrange
//    MeasurementList emptyList = new MeasurementList();
//
//    when(dependencyInjector.getUnitCollection()).thenReturn(unitCollection);
//    when(unitCollection.getUnits()).thenReturn(Collections.emptyList());
//
//    // Act
//    StoreData result = processor.processMeasurementsToStoreData(emptyList);
//
//    // Assert
//    assertNotNull(result);
//    assertEquals(0, result.size());
//  }

  // Helper methods
  private MeasurementList createTestMeasurementList() {
    MeasurementList list = new MeasurementList();
    list.add(createMeasurement("test-id-1", 100.0, Instant.now(), 1879048194));
    list.add(createMeasurement("test-id-2", 200.0, Instant.now(), 1879048194));
    return list;
  }

  private Measurement createMeasurement(String uid, double value, Instant time, int qCode) {
    return new Measurement(uid, value, time, qCode);
  }

  private StoreData createTestStoreData() {
    StoreData storeData = new StoreData();

    // Create a real Unit instance
    Unit unit = createTestUnit("Test Unit");

    // Create UnitDto from the real unit
    UnitDto unitDto = new UnitDto(unit);

    // Add to StoreData
    storeData.addUnitData(unitDto);

    return storeData;
  }

  private Unit createTestUnit() {
    return createTestUnit("Test Unit");
  }

  private Unit createTestUnit(String name) {
    JsonObject config = new JsonObject();
    config.put("наименование", name);
    config.put("группа", "Test Group");
    config.put("направление", 1);
    config.put("в работе", "да");
    config.put("проверять и МДП и АДП", "нет");
    config.put("Дельта ТИ", 10);

    JsonObject paramJson = new JsonObject()
      .put("name", "param1")
      .put("uuid", "df092d1f-b435-4873-85af-39fa675b611e");
    JsonArray parameters = new JsonArray().add(paramJson);

    config.put("исходные данные", parameters);
    config.put("Влияющие ТИ", new io.vertx.core.json.JsonArray());
    config.put("топология", new io.vertx.core.json.JsonArray());
    config.put("ТС элементов", new io.vertx.core.json.JsonArray());

    return new Unit(0, config);
  }

  private List<Unit> createTestUnits() {
    List<Unit> units = new ArrayList<>();
    units.add(createTestUnit("Unit 1"));
    units.add(createTestUnit("Unit 2"));
    return units;
  }


  // Helper class to create Measurement instances for testing
  private static class Measurement extends arbiter.measurement.Measurement {
    public Measurement(String uid, double value, Instant time, int qCode) {
      super(createJsonNode(uid, value, time, qCode));
    }

    private static com.fasterxml.jackson.databind.JsonNode createJsonNode(String uid, double value, Instant time, int qCode) {
      com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
      com.fasterxml.jackson.databind.node.ObjectNode node = mapper.createObjectNode();
      node.put("uid", uid);
      node.put("timeStamp", time.toString());
      node.put("timeStamp2", time.toString());
      node.put("qCode", qCode);
      node.put("value", value);
      return node;
    }
  }

  private UnitCollection createTestUnitCollection() {
    // Create test configuration
    JsonObject config = createTestConfig();

    // Create UnitCollection with test config
    return new TestUnitCollection(vertx, config);
  }

  private JsonObject createTestConfig() {
    JsonObject config = new JsonObject();

    // OIK settings
    JsonObject oik = new JsonObject();
    oik.put("адрес", "test-oik-address");
    oik.put("пользователь", "test-user");
    oik.put("отладка", "нет");
    config.put("ОИК", oik);

    config.put("запись в ОИК", "нет");
    config.put("изменение критерия МДП СМЗУ", "");
    config.put("запись критерия МДП СМЗУ", "");

    // Create units array
    JsonArray unitsArray = new JsonArray();
    unitsArray.add(createUnitConfig("Test Unit", 0));

    config.put("сечение", unitsArray);

    return config;
  }

  private JsonObject createUnitConfig(String unitName, int index) {
    JsonObject unitConfig = new JsonObject();
    unitConfig.put("наименование", unitName);
    unitConfig.put("группа", "Test Group");
    unitConfig.put("направление", 1);
    unitConfig.put("в работе", "да");
    unitConfig.put("проверять и МДП и АДП", "нет");
    unitConfig.put("Дельта ТИ", 10);

    // Parameters
    JsonArray paramsArray = new JsonArray();

    JsonObject param1 = new JsonObject();
    param1.put("имя", "param1");
    param1.put("id", "df092d1f-b435-4873-85af-39fa675b611e");
    param1.put("min", 0);
    param1.put("max", 100);
    paramsArray.add(param1);

    JsonObject param2 = new JsonObject();
    param2.put("имя", "Test Parameter 2");
    param2.put("id", "param-id-2");
    paramsArray.add(param2);

    unitConfig.put("исходные данные", paramsArray);

    // Topologies
    JsonArray topologyArray = new JsonArray();

    JsonObject topology1 = new JsonObject();
    topology1.put("id", "topology-id-1");
    topology1.put("имя", "Test Topology 1");
    topologyArray.add(topology1);

    unitConfig.put("топология", topologyArray);

    // Elements
    JsonArray elementArray = new JsonArray();

    JsonObject element1 = new JsonObject();
    element1.put("id", "element-id-1");
    element1.put("имя", "Test Element 1");
    elementArray.add(element1);

    unitConfig.put("ТС элементов", elementArray);

    // Influencing Factors
    JsonArray factorArray = new JsonArray();

    JsonObject factor1 = new JsonObject();
    factor1.put("id", "factor-id-1");
    factor1.put("имя", "Test Factor 1");
    factorArray.add(factor1);

    unitConfig.put("Влияющие ТИ", factorArray);

    // Repair Schema
    JsonObject repairSchema = new JsonObject();
    repairSchema.put("проверка", "test-formula");

    JsonArray tvSignals = new JsonArray();
    JsonObject signal = new JsonObject();
    signal.put("группа", 1);
    signal.put("операция", "test-operation");

    JsonArray composition = new JsonArray();
    JsonObject composition1 = new JsonObject();
    composition1.put("id", "composition-id-1");
    composition1.put("имя", "Test Composition 1");
    composition.add(composition1);

    signal.put("состав", composition);
    tvSignals.add(signal);
    repairSchema.put("телесигналы", tvSignals);

    unitConfig.put("ремонтная схема", repairSchema);

    return unitConfig;
  }
}
