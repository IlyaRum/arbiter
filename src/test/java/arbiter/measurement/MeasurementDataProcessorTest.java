package arbiter.measurement;

import arbiter.data.StoreData;
import arbiter.data.UnitCollection;
import arbiter.data.model.*;
import arbiter.di.DependencyInjector;
import io.vertx.core.Vertx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeasurementDataProcessorTest {

  @Mock
  private DependencyInjector dependencyInjector;

  @Mock
  private DataReadyCallback dataReadyCallback;

  @Mock
  private UnitCollection mockUnitCollection;

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
    String paramId = "parameterId";
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

    when(mockUnitCollection.getUnits()).thenReturn(units);
    when(dependencyInjector.getUnitCollection()).thenReturn(mockUnitCollection);

    processor.onDataReceived(list);

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
    MeasurementList firstList = new MeasurementList();

    String paramId = "parameterId";
    String paramName = "param name";
    double paramValue = 42.5;
    Instant paramTime = Instant.now();
    int qCode = 1879048194;

    Measurement measurement = createMeasurement(paramId, paramValue, paramTime, qCode);
    firstList.add(measurement);

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

    when(dependencyInjector.getUnitCollection()).thenReturn(mockUnitCollection);
    when(mockUnitCollection.getUnits()).thenReturn(units);

    processor.onDataReceived(firstList);

    verify(dataReadyCallback, times(1)).onDataReady(any(StoreData.class), isNull());
    reset(dataReadyCallback);
    verify(dataReadyCallback, times(0)).onDataReady(any(StoreData.class), isNull());

    MeasurementList secondList = new MeasurementList();
    double paramValue2 = 50.0;
    Instant paramTime2 = Instant.now();
    int qCode2 = 1879048194;

    secondList.add(createMeasurement(paramId, paramValue2, paramTime2, qCode2));
    when(parameter.isDataDifferent(eq(paramValue2), eq(paramTime2))).thenReturn(true);

    processor.onDataReceived(secondList);

    verify(dataReadyCallback, never()).onDataReady(any(StoreData.class), eq(null));
  }

  private Measurement createMeasurement(String uid, double value, Instant time, int qCode) {
    return new Measurement(uid, value, time, qCode);
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
}
