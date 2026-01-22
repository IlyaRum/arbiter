package arbiter.measurement;

import arbiter.config.TestDataConfig;
import arbiter.data.StoreData;
import arbiter.data.UnitCollection;
import arbiter.data.dto.UnitDto;
import arbiter.data.model.*;
import arbiter.di.DependencyInjector;
import arbiter.helper.MeasurementChangeTrackerReflectionTestHelper;
import arbiter.measurement.state.UnitState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class MeasurementChangeTrackerTest {

  private static final String s1 = "МДП с ПА [СМЗУ]";
  private static final String s2 = "МДП без ПА [СМЗУ]";
  private static final String s3 = "АДП [СМЗУ]";
  private static final String s4 = "Номер цикла расчета СМЗУ";

  @Mock
  private DependencyInjector dependencyInjector;

  @Mock
  private DataReadyCallback dataReadyCallback;

  @Mock
  private ExecutorService singleThreadExecutor;

  @Mock
  private UnitCollection unitCollection;

  @Captor
  private ArgumentCaptor<Runnable> runnableCaptor;

  private MeasurementChangeTracker measurementChangeTracker;
  private Map<String, Unit> mockUnits;
  private Instant testTimestamp;
  private MeasurementChangeTrackerReflectionTestHelper measurementChangeTrackerReflectionTestHelper;

  @BeforeEach
  void setUp() {
    measurementChangeTracker = new MeasurementChangeTracker(dependencyInjector, dataReadyCallback, singleThreadExecutor);
    measurementChangeTrackerReflectionTestHelper = new MeasurementChangeTrackerReflectionTestHelper(measurementChangeTracker);
    mockUnits = new HashMap<>();
    testTimestamp = Instant.now();
    when(dependencyInjector.getUnitCollection()).thenReturn(unitCollection);
    when(unitCollection.getUnits()).thenReturn(new ArrayList<>());
  }

  @Test
  void testAggregateData_InitialLoad() {
    TestDataConfig config = createUnit1Config(testTimestamp);

    Unit mockUnit = createMockUnit(config);
    when(unitCollection.getUnits()).thenReturn(List.of(mockUnit));

    List<Measurement> measurements = Arrays.asList(
      createMeasurement(config.getDp1Uid(), config.getDp1Value(), testTimestamp),
      createMeasurement(config.getDp2Uid(), config.getDp2Value(), testTimestamp),
      createMeasurement(config.getDp3Uid(), config.getDp3Value(), testTimestamp),
      createMeasurement(config.getCycleUid(), config.getCycleValue(), testTimestamp)
    );

    Map<String, Parameter> parameters = createParametersObj(config);

    StoreData storeData = createStoreDataWithUnit(config.getUnitName(), parameters, mockUnit);

    measurementChangeTracker.trackAndProcessChanges(measurements, storeData);

    Boolean initialDataLoaded = measurementChangeTrackerReflectionTestHelper.getUnitInitialDataLoaded().get(config.getUnitName());
    assertTrue(initialDataLoaded);

    Map<String, Double> previousValues = measurementChangeTrackerReflectionTestHelper.getUnitPreviousParameterValues().get(config.getUnitName());
    assertNotNull(previousValues);
  }

  @Test
  void testAggregateData_WithChanges() {
    TestDataConfig config = createUnit1Config(testTimestamp);

    Unit mockUnit = createMockUnit(config);
    when(unitCollection.getUnits()).thenReturn(List.of(mockUnit));

    Instant firstTimestamp = testTimestamp;
    List<Measurement> firstMeasurements = Arrays.asList(
      createMeasurement(config.getDp1Uid(), config.getDp1Value(), firstTimestamp),
      createMeasurement(config.getDp2Uid(), config.getDp2Value(), firstTimestamp),
      createMeasurement(config.getDp3Uid(), config.getDp3Value(), firstTimestamp),
      createMeasurement(config.getCycleUid(), config.getCycleValue(), firstTimestamp)
    );

    Map<String, Parameter> firstParameters = createParametersObj(config);

    StoreData firstStoreData = createStoreDataWithUnit(config.getUnitName(), firstParameters, mockUnit);

    measurementChangeTracker.trackAndProcessChanges(firstMeasurements, firstStoreData);

    Boolean initialDataLoader = measurementChangeTrackerReflectionTestHelper.getUnitInitialDataLoaded().get(config.getUnitName());
    assertTrue(initialDataLoader, "Initial data should be loaded after first call");

    Map<String, Double> previousValuesFirst  = measurementChangeTrackerReflectionTestHelper.getUnitPreviousParameterValues().get(config.getUnitName());
    assertNotNull(previousValuesFirst, "Previous values should be saved");
    assertEquals(config.getDp1Value(), previousValuesFirst.get(config.getDp1Uid()), 0.001, "Previous value for param1 should be " + config.getDp1Value());
    assertEquals(config.getDp2Value(), previousValuesFirst.get(config.getDp2Uid()), 0.001, "Previous value for param2 should be " + config.getDp2Value());
    assertEquals(config.getDp3Value(), previousValuesFirst.get(config.getDp3Uid()), 0.001, "Previous value for param3 should be " + config.getDp3Value());
    assertEquals(config.getCycleValue(), previousValuesFirst.get(config.getCycleUid()), 0.001, "Previous value for CycleParam should be " + config.getCycleValue());

    Instant secondTimestamp = firstTimestamp.plusSeconds(2);

    TestDataConfig newConfig = config.setDp1Value(2800.0).setDp2Value(2032.26).setDp3Value(4357.10).setCycleValue(26320.0).setTimeStamp(secondTimestamp);

    List<Measurement> secondMeasurements = Arrays.asList(
      createMeasurement(config.getDp1Uid(), newConfig.getDp1Value(), secondTimestamp),
      createMeasurement(config.getDp2Uid(), newConfig.getDp2Value(), secondTimestamp),
      createMeasurement(config.getDp3Uid(), newConfig.getDp3Value(), secondTimestamp),
      createMeasurement(config.getCycleUid(), newConfig.getCycleValue(), secondTimestamp)
    );

    Map<String, Parameter> secondParameters = createParametersObj(newConfig);

    StoreData secondStoreData = createStoreDataWithUnit(config.getUnitName(), secondParameters, mockUnit);

    doAnswer(invocation -> {
      Runnable task = invocation.getArgument(0);
      task.run();
      return null;
    }).when(singleThreadExecutor).submit(any(Runnable.class));

    measurementChangeTracker.trackAndProcessChanges(secondMeasurements, secondStoreData);

    Map<String, Double> previousValuesSecond  = measurementChangeTrackerReflectionTestHelper.getUnitPreviousParameterValues().get(config.getUnitName());
    assertNotNull(previousValuesSecond, "Previous values should be saved");
    assertEquals(newConfig.getDp1Value(), previousValuesFirst.get(newConfig.getDp1Uid()), 0.001, "Previous value for param1 should be " + newConfig.getDp1Value());
    assertEquals(newConfig.getDp2Value(), previousValuesFirst.get(newConfig.getDp2Uid()), 0.001, "Previous value for param2 should be " + newConfig.getDp2Value());
    assertEquals(newConfig.getDp3Value(), previousValuesFirst.get(newConfig.getDp3Uid()), 0.001, "Previous value for param3 should be " + newConfig.getDp3Value());

    Map<String, Parameter> trackedChanges = measurementChangeTrackerReflectionTestHelper.getUnitTrackedChanges().get(config.getUnitName());
    assertTrue(trackedChanges == null || trackedChanges.isEmpty(),
      "Tracked changes should be cleared after sending");

    verify(singleThreadExecutor, times(1)).submit(any(Runnable.class));
    verify(dataReadyCallback, times(1)).onDataReady(any(StoreData.class), eq(config.getUnitName()));
  }

  @Test
  void testAggregateData_MultipleUnits() {
    TestDataConfig unit1Config = createUnit1Config(testTimestamp);
    TestDataConfig unit2Config = createUnit2Config(testTimestamp);

    Unit mockUnit1 = createMockUnit(unit1Config);
    Unit mockUnit2 = createMockUnit(unit2Config);

    when(unitCollection.getUnits()).thenReturn(List.of(mockUnit1, mockUnit2));

    List<Measurement> measurements = Arrays.asList(
      createMeasurement(unit1Config.getDp1Uid(), unit1Config.getDp1Value(), testTimestamp),
      createMeasurement(unit1Config.getDp2Uid(), unit1Config.getDp2Value(), testTimestamp),
      createMeasurement(unit1Config.getDp3Uid(), unit1Config.getDp3Value(), testTimestamp),
      createMeasurement(unit1Config.getCycleUid(), unit1Config.getCycleValue(), testTimestamp),
      createMeasurement(unit2Config.getDp1Uid(), unit2Config.getDp1Value(), testTimestamp),
      createMeasurement(unit2Config.getDp2Uid(), unit2Config.getDp2Value(), testTimestamp),
      createMeasurement(unit2Config.getDp3Uid(), unit2Config.getDp3Value(), testTimestamp),
      createMeasurement(unit2Config.getCycleUid(), unit2Config.getCycleValue(), testTimestamp)
    );

    StoreData storeData = new StoreData();
    UnitDto unitDto1 = new UnitDto(mockUnit1);
    unitDto1.getParameters();
    UnitDto unitDto2 = new UnitDto(mockUnit2);
    storeData.addUnitData(unitDto1);
    storeData.addUnitData(unitDto2);

    measurementChangeTracker.trackAndProcessChanges(measurements, storeData);

    assertTrue(measurementChangeTrackerReflectionTestHelper.getUnitInitialDataLoaded().containsKey(unit1Config.getUnitName()));
    assertTrue(measurementChangeTrackerReflectionTestHelper.getUnitInitialDataLoaded().containsKey(unit2Config.getUnitName()));
  }

  @Test
  void testAggregateData_SpecialValue99999() {
    TestDataConfig config = createUnit1Config(testTimestamp);

    config.setDp1Value(99999.0);

    Unit mockUnit = createMockUnit(config);
    when(unitCollection.getUnits()).thenReturn(List.of(mockUnit));

    Instant firstTimestamp = testTimestamp;
    List<Measurement> firstMeasurements = Arrays.asList(
      createMeasurement(config.getDp1Uid(), config.getDp1Value(), firstTimestamp),
      createMeasurement(config.getDp2Uid(), config.getDp2Value(), firstTimestamp),
      createMeasurement(config.getDp3Uid(), config.getDp3Value(), firstTimestamp),
      createMeasurement(config.getCycleUid(), config.getCycleValue(), firstTimestamp)
    );

    Map<String, Parameter> parameters = createParametersObj(config);

    StoreData firstStoreData = createStoreDataWithUnit(config.getUnitName(), parameters, mockUnit);

    measurementChangeTracker.trackAndProcessChanges(firstMeasurements, firstStoreData);


    Instant secondTimestamp = firstTimestamp.plusSeconds(2);
    TestDataConfig newConfig = config.setDp1Value(99999.0).setDp2Value(1234.0).setDp3Value(5679.0).setCycleValue(26320.0).setTimeStamp(secondTimestamp);
    List<Measurement> secondMeasurements = Arrays.asList(
      createMeasurement(newConfig.getDp1Uid(), newConfig.getDp1Value(), secondTimestamp),
      createMeasurement(newConfig.getDp2Uid(), newConfig.getDp2Value(), secondTimestamp),
      createMeasurement(newConfig.getDp3Uid(), newConfig.getDp3Value(), secondTimestamp),
      createMeasurement(newConfig.getCycleUid(), newConfig.getCycleValue(), secondTimestamp)
    );

    Map<String, Parameter> secondParameters = createParametersObj(newConfig);

    StoreData secondStoreData = createStoreDataWithUnit(newConfig.getUnitName(), secondParameters, mockUnit);

    doAnswer(invocation -> {
      Runnable task = invocation.getArgument(0);
      task.run();
      return null;
    }).when(singleThreadExecutor).submit(any(Runnable.class));

    measurementChangeTracker.trackAndProcessChanges(secondMeasurements, secondStoreData);

    Map<String, Double> previousValues  = measurementChangeTrackerReflectionTestHelper.getUnitPreviousParameterValues().get(newConfig.getUnitName());
    assertNotNull(previousValues, "Previous values should be saved");
    assertEquals(99999.0, previousValues.get(newConfig.getDp1Uid()), 0.001, "Previous value for param1 should be 99999.0");

    verify(singleThreadExecutor, times(1)).submit(runnableCaptor.capture());
    verify(dataReadyCallback, times(1)).onDataReady(any(StoreData.class), eq(newConfig.getUnitName()));
  }

  @Test
  void testProcessConsistentData_SameTimestamp() {

  }

  @Test
  void testCreateMeasurementsMap() {

    TestDataConfig config = createUnit1Config(testTimestamp);

    List<Measurement> measurements = Arrays.asList(
      createMeasurement(config.getDp1Uid(), config.getDp1Value(), testTimestamp),
      createMeasurement(config.getDp2Uid(), config.getDp2Value(), testTimestamp),
      createMeasurement(config.getDp3Uid(), config.getDp3Value(), testTimestamp)
    );

    Map<String, Measurement> result = measurementChangeTrackerReflectionTestHelper.invokeCreateMeasurementsMap(measurements);

    assertNotNull(result);
    assertEquals(3, result.size());
    assertEquals(config.getDp1Value(), result.get(config.getDp1Uid()).getValue());
    assertEquals(config.getDp2Value(), result.get(config.getDp2Uid()).getValue());
    assertEquals(config.getDp3Value(), result.get(config.getDp3Uid()).getValue());
  }

  @Test
  void testAggregateData_CallbackThrowsException() throws Exception {
    TestDataConfig config = createUnit1Config(testTimestamp);

    Unit mockUnit = createMockUnit(config);
    when(unitCollection.getUnits()).thenReturn(List.of(mockUnit));

    Map<String, Parameter> trackedChanges = createParametersObj(config);
    Map<String, Topology> trackedTopologyChanges = new ConcurrentHashMap<>();
    Map<String, Element> trackedElementChanges = new ConcurrentHashMap<>();
    Map<String, InfluencingFactor> trackedFactorChanges = new ConcurrentHashMap<>();
    Map<String, Composition> trackedCompositionChanges = new ConcurrentHashMap<>();

    measurementChangeTrackerReflectionTestHelper.getUnitTrackedChanges().put(config.getUnitName(), trackedChanges);
    measurementChangeTrackerReflectionTestHelper.getUnitTrackedTopologyChanges().put(config.getUnitName(), trackedTopologyChanges);
    measurementChangeTrackerReflectionTestHelper.getUnitTrackedElementChanges().put(config.getUnitName(), trackedElementChanges);
    measurementChangeTrackerReflectionTestHelper.getUnitTrackedFactorChanges().put(config.getUnitName(), trackedFactorChanges);
    measurementChangeTrackerReflectionTestHelper.getUnitTrackedRepairChanges().put(config.getUnitName(), trackedCompositionChanges);

    Map<String, Double> previousParameterValues = new ConcurrentHashMap<>();
    previousParameterValues.put(config.getDp1Uid(), config.getDp1Value());
    previousParameterValues.put(config.getDp2Uid(), config.getDp2Value());
    previousParameterValues.put(config.getDp3Uid(), config.getDp3Value());

    measurementChangeTrackerReflectionTestHelper.getUnitPreviousParameterValues().put(config.getUnitName(), previousParameterValues);

    doThrow(new RuntimeException("Test callback exception"))
      .when(dataReadyCallback).onDataReady(any(StoreData.class), anyString());

    doAnswer(invocation -> {
      Runnable task = invocation.getArgument(0);
      task.run();
      return null;
    }).when(singleThreadExecutor).submit(any(Runnable.class));

    measurementChangeTrackerReflectionTestHelper.invokeSendTrackedChanges(config.getUnitName(), createUnitState(trackedChanges, trackedTopologyChanges,trackedElementChanges,trackedFactorChanges, trackedCompositionChanges));

    verify(singleThreadExecutor, times(1)).submit(any(Runnable.class));
    verify(dataReadyCallback, times(1)).onDataReady(any(StoreData.class), eq(config.getUnitName()));

    assertTrue(measurementChangeTrackerReflectionTestHelper.getUnitTrackedChanges().get(config.getUnitName()).isEmpty(),"Tracked parameter changes should be cleared even when executor throws exception");
    assertTrue(measurementChangeTrackerReflectionTestHelper.getUnitTrackedTopologyChanges().get(config.getUnitName()).isEmpty(),"Tracked topology changes should be cleared even when executor throws exception");
    assertTrue(measurementChangeTrackerReflectionTestHelper.getUnitTrackedElementChanges().get(config.getUnitName()).isEmpty(),"Tracked elements changes should be cleared even when executor throws exception");
    assertTrue(measurementChangeTrackerReflectionTestHelper.getUnitTrackedFactorChanges().get(config.getUnitName()).isEmpty(),"Tracked influencing factor changes should be cleared even when executor throws exception");
  }

  public static TestDataConfig createUnit1Config(Instant timestamp){
    String unitName = "Волга-Урал";
    String cycleUid = "c37ba28d-392f-4c64-be65-6dfbd89fce6e";
    String dp1Uid = "1e0c7b22-e5b9-452d-a043-f2207b6c996c";
    String dp2Uid = "faadf465-6d90-47ce-8f97-9ce6a67f6e09";
    String dp3Uid = "df69d232-1099-4a43-97d8-cfdb0672748d";

    Set<String> targetUids = Set.of(dp1Uid, dp2Uid, dp3Uid, cycleUid);

    return TestDataConfig.builder()
      .unitName(unitName)
      .dp1Uid(dp1Uid)
      .dp2Uid(dp2Uid)
      .dp3Uid(dp3Uid)
      .cycleUid(cycleUid)
      .targetUids(targetUids)
      .timestamp(timestamp)
      .build();
  }

  public static TestDataConfig createUnit2Config(Instant timestamp){
    String unitName = "Сечение 1";
    String cycleUid = "834c528a-d26d-4cf5-abeb-cec9ad378b5c";
    String dp1Uid = "d4bd8c04-c399-4360-8756-dc123ce70ee1";
    String dp2Uid = "8f805a91-eeef-46f2-968e-ef043658f8d8";
    String dp3Uid = "a7ec7d66-5460-44af-abdd-9fceb8e2af5c";

    Set<String> targetUids = Set.of(dp1Uid, dp2Uid, dp3Uid, cycleUid);

    return TestDataConfig.builder()
      .unitName(unitName)
      .dp1Uid(dp1Uid)
      .dp2Uid(dp2Uid)
      .dp3Uid(dp3Uid)
      .cycleUid(cycleUid)
      .targetUids(targetUids)
      .timestamp(timestamp)
      .build();
  }

  private static Map<String, Parameter> createParametersObj(TestDataConfig config) {
    Map<String, Parameter> parameters = new HashMap<>();
    Parameter param1 = new Parameter(s1, config.getDp1Uid());
    param1.setData(config.getDp1Value(), config.getTimestamp(), 0);
    parameters.put(s1, param1);
    Parameter param2 = new Parameter(s2, config.getDp2Uid());
    param2.setData(config.getDp2Value(), config.getTimestamp(), 0);
    parameters.put(s2, param2);
    Parameter param3 = new Parameter(s3, config.getDp3Uid());
    param3.setData(config.getDp3Value(), config.getTimestamp(), 0);
    parameters.put(s3, param3);
    Parameter cycleParam = new Parameter(s4, config.getCycleUid());
    cycleParam.setData(config.getCycleValue(), config.getTimestamp(),0);
    parameters.put(s4, cycleParam);
    return parameters;
  }

  private UnitState createUnitState(Map<String, Parameter> trackedChanges,
                                    Map<String, Topology> trackedTopologyChanges,
                                    Map<String, Element> trackedElementChanges,
                                    Map<String, InfluencingFactor> trackedInfluencingFactorChanges,
                                    Map<String, Composition> trackedRepairChanges) {
    return new UnitState(trackedChanges, trackedTopologyChanges,
      trackedElementChanges, trackedInfluencingFactorChanges,trackedRepairChanges
    );
  }

  private Unit createMockUnit(TestDataConfig config) {
    Unit mockUnit = mock(Unit.class);
    when(mockUnit.getName()).thenReturn(config.getUnitName());

    List<Parameter> parameters = new ArrayList<>();
    parameters.add(new Parameter(s1, config.getDp1Uid()));
    parameters.add(new Parameter(s2, config.getDp2Uid()));
    parameters.add(new Parameter(s3, config.getDp3Uid()));
    parameters.add(new Parameter(s4, config.getCycleUid()));

    lenient().when(mockUnit.getParameters()).thenReturn(parameters);

    when(unitCollection.getCycleNumberUidFromUnit(mockUnit)).thenReturn(config.getCycleUid());
    when(unitCollection.getTargetUidsForUnit(mockUnit)).thenReturn(config.getTargetUids());

    mockUnits.put(config.getUnitName(), mockUnit);
    return mockUnit;
  }

  private Measurement createMeasurement(String uid, double value, Instant timestamp) {
    Measurement measurement = mock(Measurement.class);
    when(measurement.getUid()).thenReturn(uid);
    when(measurement.getValue()).thenReturn(value);
    when(measurement.getTimeStamp()).thenReturn(timestamp.toString());
    return measurement;
  }

  private StoreData createStoreDataWithUnit(String unitName, Map<String, Parameter> parameters, Unit unit) {
    StoreData storeData = new StoreData();

    Unit unitToUse = (unit != null) ? unit : mock(Unit.class);
    if (unit == null) {
      when(unitToUse.getName()).thenReturn(unitName);
    }

    List<Parameter> unitParameters = new ArrayList<>(parameters.values());
    when(unitToUse.getParameters()).thenReturn(unitParameters);

    UnitDto unitDto = new UnitDto(unitToUse);
    for (Map.Entry<String, Parameter> entry : parameters.entrySet()) {
      unitDto.getParameters().put(entry.getKey(), entry.getValue());
    }

    storeData.addUnitData(unitDto);
    return storeData;
  }

}
