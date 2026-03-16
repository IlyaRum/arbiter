package arbiter.measurement;

import arbiter.data.StoreData;
import arbiter.data.UnitCollection;
import arbiter.data.model.*;
import arbiter.di.DependencyInjector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeasurementDataProcessorTest {

  private static final String UNIT_NAME = "unit name";
  private static final String PARAM_NAME = "param name";
  private static final String PARAM_ID = "test-parameter-id";
  private static final String TOPOLOGY_ID = "test-topology-id";
  private static final String ELEMENT_ID = "test-element-id";
  private static final String FACTOR_ID = "test-factor-id";
  private static final String COMPOSITION_ID = "test-composition-id";
  private static final int Q_CODE = 1879048194;

  @Mock
  private DependencyInjector dependencyInjector;

  @Mock
  private DataReadyCallback dataReadyCallback;

  @Mock
  private UnitCollection mockUnitCollection;

  @Mock
  private ExecutorService executorMock;

  @Captor
  private ArgumentCaptor<Runnable> runnableCaptor;

  @Captor
  private ArgumentCaptor<StoreData> storeDataCaptor;

  @Captor
  private ArgumentCaptor<String> stringCaptor;

  private MeasurementDataProcessor processor;
  private Instant currentTime;

  @BeforeEach
  void setUp() {
    processor = new MeasurementDataProcessor(dependencyInjector, executorMock);
    processor.setDataReadyCallback(dataReadyCallback);
    currentTime = Instant.now();
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
    when(dependencyInjector.getUnitCollection()).thenThrow(new RuntimeException("Test exception"));

    assertDoesNotThrow(() -> processor.onDataReceived(list));
    verify(dataReadyCallback, never()).onDataReady(any(StoreData.class), any());
    verify(executorMock, never()).submit(any(Runnable.class));
  }

  @Test
  void testOnDataReceived_FirstCall_ShouldCaptureSnapshotAndProcessMeasurements() {
    double paramValue = 42.5;
    Measurement measurement = createMeasurement(PARAM_ID, paramValue, currentTime, Q_CODE);
    MeasurementList list = createMeasurementList(measurement);

    Unit unit = createMockUnit();
    Parameter parameter = createMockParameter(PARAM_ID, PARAM_NAME);
    when(parameter.isDataDifferent(eq(paramValue), eq(currentTime))).thenReturn(true);

    List<Parameter> parameters = new ArrayList<>();
    parameters.add(parameter);
    setupUnitWithParameters(unit, parameters);

    List<Unit> units = new ArrayList<>();
    units.add(unit);
    setupDependencyInjectorWithUnits(units);

    processor.onDataReceived(list);

    verify(executorMock, times(1)).submit(runnableCaptor.capture());

    verify(parameter).setData(eq(paramValue), eq(currentTime), eq(Q_CODE));

    assertFalse(processor.isFirstTime());
  }

  @Test
  void testOnDataReceived_FirstCall_ShouldSendPostRequestWithSnapshot() {
    double paramValue = 42.5;
    Measurement measurement = createMeasurement(PARAM_ID, paramValue, currentTime, Q_CODE);
    MeasurementList list = createMeasurementList(measurement);

    Unit unit = createMockUnit();
    Parameter parameter = createMockParameter(PARAM_ID, PARAM_NAME);
    when(parameter.isDataDifferent(eq(paramValue), eq(currentTime))).thenReturn(true);

    List<Parameter> parameters = new ArrayList<>();
    parameters.add(parameter);
    setupUnitWithParameters(unit, parameters);

    List<Unit> units = new ArrayList<>();
    units.add(unit);
    setupDependencyInjectorWithUnits(units);

    processor.onDataReceived(list);

    verify(executorMock, times(1)).submit(runnableCaptor.capture());

    Runnable postTask = runnableCaptor.getValue();
    postTask.run();

    verify(dataReadyCallback, times(1)).onDataReady(storeDataCaptor.capture(), isNull());

    StoreData capturedData = storeDataCaptor.getValue();
    assertNotNull(capturedData);
    assertEquals(1, capturedData.size());
  }

  @Test
  void testOnDataReceived_SecondCall_ShouldNotSendPostRequest() {
    double firstValue = 42.5;
    double secondValue = 50.0;
    Instant secondTime = Instant.now();

    Measurement firstMeasurement = createMeasurement(PARAM_ID, firstValue, currentTime, Q_CODE);
    MeasurementList firstList = createMeasurementList(firstMeasurement);

    Measurement secondMeasurement = createMeasurement(PARAM_ID, secondValue, secondTime, Q_CODE);
    MeasurementList secondList = createMeasurementList(secondMeasurement);

    Unit unit = createMockUnit();
    Parameter parameter = createMockParameter(PARAM_ID, PARAM_NAME);
    when(parameter.isDataDifferent(anyDouble(), any(Instant.class))).thenReturn(true);

    List<Parameter> parameters = new ArrayList<>();
    parameters.add(parameter);
    setupUnitWithParameters(unit, parameters);

    List<Unit> units = new ArrayList<>();
    units.add(unit);
    setupDependencyInjectorWithUnits(units);

    processor.onDataReceived(firstList);

    reset(executorMock, dataReadyCallback);

    processor.onDataReceived(secondList);

    verify(executorMock, never()).submit(any(Runnable.class));
    verify(dataReadyCallback, never()).onDataReady(any(StoreData.class), isNull());
  }

  @Test
  void testOnDataReceived_MultipleCalls_ShouldProcessAllMeasurements() {
    double firstValue = 42.5;
    double secondValue = 50.0;
    Instant secondTime = Instant.now();

    Measurement firstMeasurement = createMeasurement(PARAM_ID, firstValue, currentTime, Q_CODE);
    MeasurementList firstList = createMeasurementList(firstMeasurement);

    Measurement secondMeasurement = createMeasurement(PARAM_ID, secondValue, secondTime, Q_CODE);
    MeasurementList secondList = createMeasurementList(secondMeasurement);

    Unit unit = createMockUnit();
    Parameter parameter = createMockParameter(PARAM_ID, PARAM_NAME);
    when(parameter.isDataDifferent(eq(firstValue), eq(currentTime))).thenReturn(true);
    when(parameter.isDataDifferent(eq(secondValue), eq(secondTime))).thenReturn(true);

    List<Parameter> parameters = new ArrayList<>();
    parameters.add(parameter);
    setupUnitWithParameters(unit, parameters);

    List<Unit> units = new ArrayList<>();
    units.add(unit);
    setupDependencyInjectorWithUnits(units);

    processor.onDataReceived(firstList);
    processor.onDataReceived(secondList);

    verify(parameter, times(2)).setData(anyDouble(), any(Instant.class), anyInt());
  }

  @Test
  void testOnDataReceived_WithMeasurement_ProcessesParameters() {
    double paramValue = 42.5;
    Measurement measurement = createMeasurement(PARAM_ID, paramValue, currentTime, Q_CODE);
    MeasurementList list = createMeasurementList(measurement);

    Unit unit = createMockUnit();
    Parameter parameter = createMockParameter(PARAM_ID, PARAM_NAME);
    when(parameter.isDataDifferent(eq(paramValue), eq(currentTime))).thenReturn(true);

    List<Parameter> parameters = new ArrayList<>();
    parameters.add(parameter);
    setupUnitWithParameters(unit, parameters);

    List<Unit> units = new ArrayList<>();
    units.add(unit);
    setupDependencyInjectorWithUnits(units);

    processor.onDataReceived(list);

    verify(parameter).setData(eq(paramValue), eq(currentTime), eq(Q_CODE));
  }

  @Test
  void testOnDataReceived_WithMeasurement_ProcessesTopologies() {
    double topologyValue = 1.0;
    Measurement measurement = createMeasurement(TOPOLOGY_ID, topologyValue, currentTime, Q_CODE);
    MeasurementList list = createMeasurementList(measurement);

    Unit unit = createMockUnit();
    Topology topology = createMockTopology(TOPOLOGY_ID);
    when(topology.isDataDifferent(eq(topologyValue), eq(currentTime))).thenReturn(true);

    List<Topology> topologies = new ArrayList<>();
    topologies.add(topology);
    setupUnitWithTopologies(unit, topologies);

    List<Unit> units = new ArrayList<>();
    units.add(unit);
    setupDependencyInjectorWithUnits(units);

    processor.onDataReceived(list);

    verify(topology).setData(eq(topologyValue), eq(currentTime), eq(Q_CODE));
  }

  @Test
  void testOnDataReceived_WithMeasurement_ProcessesElements() {
    double elementValue = 10.5;
    Measurement measurement = createMeasurement(ELEMENT_ID, elementValue, currentTime, Q_CODE);
    MeasurementList list = createMeasurementList(measurement);

    Unit unit = createMockUnit();
    Element element = createMockElement(ELEMENT_ID);
    when(element.isDataDifferent(eq(elementValue), eq(currentTime))).thenReturn(true);

    List<Element> elements = new ArrayList<>();
    elements.add(element);
    setupUnitWithElements(unit, elements);

    List<Unit> units = new ArrayList<>();
    units.add(unit);
    setupDependencyInjectorWithUnits(units);

    processor.onDataReceived(list);

    verify(element).setData(eq(elementValue), eq(currentTime), eq(Q_CODE));
  }

  @Test
  void testOnDataReceived_WithMeasurement_ProcessesInfluencingFactors() {
    double factorValue = 5.5;
    Measurement measurement = createMeasurement(FACTOR_ID, factorValue, currentTime, Q_CODE);
    MeasurementList list = createMeasurementList(measurement);

    Unit unit = createMockUnit();
    InfluencingFactor factor = createMockInfluencingFactor(FACTOR_ID);
    when(factor.isDataDifferent(eq(factorValue), eq(currentTime))).thenReturn(true);

    List<InfluencingFactor> factors = new ArrayList<>();
    factors.add(factor);
    setupUnitWithInfluencingFactors(unit, factors);

    List<Unit> units = new ArrayList<>();
    units.add(unit);
    setupDependencyInjectorWithUnits(units);

    processor.onDataReceived(list);

    verify(factor).setData(eq(factorValue), eq(currentTime), eq(Q_CODE));
  }

  @Test
  void testOnDataReceived_WithMeasurement_ProcessesRepairSchema() {
    double compositionValue = 3.5;
    Measurement measurement = createMeasurement(COMPOSITION_ID, compositionValue, currentTime, Q_CODE);
    MeasurementList list = createMeasurementList(measurement);

    Unit unit = createMockUnit();
    RepairSchema repairSchema = createMockRepairSchema(COMPOSITION_ID, compositionValue, currentTime);
    setupUnitWithRepairSchema(unit, repairSchema);

    List<Unit> units = new ArrayList<>();
    units.add(unit);
    setupDependencyInjectorWithUnits(units);

    processor.onDataReceived(list);

    Composition composition = repairSchema.getRepairGroupValues().get(0).getValues().get(0);
    verify(composition).setData(eq(compositionValue), eq(currentTime), eq(Q_CODE));
  }

  @Test
  void testOnDataReceived_ExceptionInProcessing_LogsError() {
    MeasurementList list = createMeasurementList(createMeasurement("test-id", 1.0, Instant.now(), 1));
    when(dependencyInjector.getUnitCollection()).thenThrow(new RuntimeException("Test exception"));

    assertDoesNotThrow(() -> processor.onDataReceived(list));
  }

  private Measurement createMeasurement(String uid, double value, Instant time, int qCode) {
    return new Measurement(uid, value, time, qCode);
  }

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

  private void setupDependencyInjectorWithUnits(List<Unit> units) {
    when(mockUnitCollection.getUnits()).thenReturn(units);
    when(dependencyInjector.getUnitCollection()).thenReturn(mockUnitCollection);
  }

  private static Parameter createMockParameter(String paramId, String paramName) {
    Parameter parameter = mock(Parameter.class);
    when(parameter.getId()).thenReturn(paramId);
    when(parameter.getName()).thenReturn(paramName);
    return parameter;
  }

  private static Topology createMockTopology(String id) {
    Topology topology = mock(Topology.class);
    when(topology.getId()).thenReturn(id);
    return topology;
  }

  private static Element createMockElement(String elementId) {
    Element element = mock(Element.class);
    when(element.getId()).thenReturn(elementId);
    return element;
  }

  private static InfluencingFactor createMockInfluencingFactor(String id) {
    InfluencingFactor factor = mock(InfluencingFactor.class);
    when(factor.getId()).thenReturn(id);
    return factor;
  }

  private RepairSchema createMockRepairSchema(String compositionId, double compositionValue, Instant time) {
    RepairSchema repairSchema = mock(RepairSchema.class);
    RepairGroupValue repairGroupValue = mock(RepairGroupValue.class);
    Composition composition = mock(Composition.class);

    when(composition.getId()).thenReturn(compositionId);
    when(composition.isDataDifferent(eq(compositionValue), eq(time))).thenReturn(true);

    List<Composition> compositions = new ArrayList<>();
    compositions.add(composition);

    when(repairGroupValue.getValues()).thenReturn(compositions);

    List<RepairGroupValue> repairGroupValues = new ArrayList<>();
    repairGroupValues.add(repairGroupValue);

    when(repairSchema.getRepairGroupValues()).thenReturn(repairGroupValues);
    return repairSchema;
  }

  private static Unit createMockUnit() {
    Unit unit = mock(Unit.class);
    when(unit.getName()).thenReturn(UNIT_NAME);
    return unit;
  }

  private static MeasurementList createMeasurementList(Measurement measurement) {
    MeasurementList list = new MeasurementList();
    list.add(measurement);
    return list;
  }

  private void setupUnitWithParameters(Unit unit, List<Parameter> parameters) {
    when(unit.getParameters()).thenReturn(parameters);
    when(unit.getTopologies()).thenReturn(Collections.emptyList());
    when(unit.getElements()).thenReturn(Collections.emptyList());
    when(unit.getInfluencingFactors()).thenReturn(Collections.emptyList());
    when(unit.getRepairSchema()).thenReturn(null);
  }

  private void setupUnitWithTopologies(Unit unit, List<Topology> topologies) {
    when(unit.getParameters()).thenReturn(Collections.emptyList());
    when(unit.getTopologies()).thenReturn(topologies);
    when(unit.getElements()).thenReturn(Collections.emptyList());
    when(unit.getInfluencingFactors()).thenReturn(Collections.emptyList());
    when(unit.getRepairSchema()).thenReturn(null);
  }

  private void setupUnitWithElements(Unit unit, List<Element> elements) {
    when(unit.getParameters()).thenReturn(Collections.emptyList());
    when(unit.getTopologies()).thenReturn(Collections.emptyList());
    when(unit.getElements()).thenReturn(elements);
    when(unit.getInfluencingFactors()).thenReturn(Collections.emptyList());
    when(unit.getRepairSchema()).thenReturn(null);
  }

  private void setupUnitWithInfluencingFactors(Unit unit, List<InfluencingFactor> factors) {
    when(unit.getParameters()).thenReturn(Collections.emptyList());
    when(unit.getTopologies()).thenReturn(Collections.emptyList());
    when(unit.getElements()).thenReturn(Collections.emptyList());
    when(unit.getInfluencingFactors()).thenReturn(factors);
    when(unit.getRepairSchema()).thenReturn(null);
  }

  private void setupUnitWithRepairSchema(Unit unit, RepairSchema repairSchema) {
    when(unit.getParameters()).thenReturn(Collections.emptyList());
    when(unit.getTopologies()).thenReturn(Collections.emptyList());
    when(unit.getElements()).thenReturn(Collections.emptyList());
    when(unit.getInfluencingFactors()).thenReturn(Collections.emptyList());
    when(unit.getRepairSchema()).thenReturn(repairSchema);
  }
}
