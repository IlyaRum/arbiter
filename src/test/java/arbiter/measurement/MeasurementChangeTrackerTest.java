package arbiter.measurement;

import arbiter.config.TestDataConfig;
import arbiter.data.StoreData;
import arbiter.data.UnitCollection;
import arbiter.data.dto.CommonFieldDto;
import arbiter.data.dto.UnitDto;
import arbiter.data.model.*;
import arbiter.di.DependencyInjector;
import arbiter.helper.MeasurementChangeTrackerReflectionTestHelper;
import arbiter.measurement.state.UnitState;
import io.vertx.core.internal.logging.LoggerFactory;
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
  void testProcessAndTrackChanges_InitialLoad() {
    TestDataConfig config = createUnit1Config(Instant.now());

    Unit mockUnit = createMockUnit(config);
    when(unitCollection.getUnits()).thenReturn(List.of(mockUnit));
    when(unitCollection.getCommonFieldDto()).thenReturn(new CommonFieldDto());

    MeasurementList list = createMeasurementList(Arrays.asList(
      createMeasurement(config.getDp1Uid(), 2706.18, testTimestamp),
      createMeasurement(config.getDp2Uid(), 2032.25, testTimestamp),
      createMeasurement(config.getDp3Uid(), 4357.09, testTimestamp),
      createMeasurement(config.getCycleUid(), 26319.0, testTimestamp)
    ));

    doAnswer(invocation -> {
      Runnable task = invocation.getArgument(0);
      task.run();
      return null;
    }).when(singleThreadExecutor).submit(any(Runnable.class));

    measurementChangeTracker.processAndTrackChanges(list);

    Boolean initialDataLoaded = measurementChangeTrackerReflectionTestHelper.getUnitInitialDataLoaded().get(config.getUnitName());
    assertTrue(initialDataLoaded);

    Map<String, Double> previousValues = measurementChangeTrackerReflectionTestHelper.getUnitPreviousParameterValues().get(config.getUnitName());
    assertNotNull(previousValues);
  }

  @Test
  void testProcessAndTrackChanges_WithChanges() {
    TestDataConfig config = createUnit1Config(Instant.now());

    Unit mockUnit = createMockUnit(config);
    when(unitCollection.getUnits()).thenReturn(List.of(mockUnit));
    when(unitCollection.getCommonFieldDto()).thenReturn(new CommonFieldDto());

    Instant firstTimestamp = testTimestamp;
    MeasurementList firstList = createMeasurementList(Arrays.asList(
      createMeasurement(config.getDp1Uid(), 2706.18, firstTimestamp),
      createMeasurement(config.getDp2Uid(), 2032.25, firstTimestamp),
      createMeasurement(config.getDp3Uid(), 4357.09, firstTimestamp),
      createMeasurement(config.getCycleUid(), 26319.0, firstTimestamp)
    ));

    doAnswer(invocation -> {
      Runnable task = invocation.getArgument(0);
      task.run();
      return null;
    }).when(singleThreadExecutor).submit(any(Runnable.class));

    measurementChangeTracker.processAndTrackChanges(firstList);

    Boolean initialDataLoader = measurementChangeTrackerReflectionTestHelper.getUnitInitialDataLoaded().get(config.getUnitName());
    assertTrue(initialDataLoader, "Initial data should be loaded after first call");

    Map<String, Double> previousValuesFirst = measurementChangeTrackerReflectionTestHelper.getUnitPreviousParameterValues().get(config.getUnitName());
    assertNotNull(previousValuesFirst, "Previous values should be saved");

    Instant secondTimestamp = firstTimestamp.plusSeconds(2);
    TestDataConfig newConfig = config.setDp1Value(2800.0).setDp2Value(2032.26).setDp3Value(4357.10).setCycleValue(26320.0).setTimeStamp(secondTimestamp);

    MeasurementList secondList = createMeasurementList(Arrays.asList(
      createMeasurement(config.getDp1Uid(), newConfig.getDp1Value(), secondTimestamp),
      createMeasurement(config.getDp2Uid(), newConfig.getDp2Value(), secondTimestamp),
      createMeasurement(config.getDp3Uid(), newConfig.getDp3Value(), secondTimestamp),
      createMeasurement(config.getCycleUid(), newConfig.getCycleValue(), secondTimestamp)
    ));

    measurementChangeTracker.processAndTrackChanges(secondList);

    verify(singleThreadExecutor, times(1)).submit(any(Runnable.class));
    verify(dataReadyCallback, times(1)).onDataReady(any(StoreData.class), eq(config.getUnitName()));
  }

  @Test
  void testProcessAndTrackChanges_MultipleUnits() {
    TestDataConfig unit1Config = createUnit1Config(testTimestamp);
    TestDataConfig unit2Config = createUnit2Config(testTimestamp);

    Unit mockUnit1 = createMockUnit(unit1Config);
    Unit mockUnit2 = createMockUnit(unit2Config);

    when(unitCollection.getUnits()).thenReturn(List.of(mockUnit1, mockUnit2));
    when(unitCollection.getCommonFieldDto()).thenReturn(new CommonFieldDto());

    MeasurementList list = createMeasurementList(Arrays.asList(
      createMeasurement(unit1Config.getDp1Uid(), unit1Config.getDp1Value(), testTimestamp),
      createMeasurement(unit1Config.getDp2Uid(), unit1Config.getDp2Value(), testTimestamp),
      createMeasurement(unit1Config.getDp3Uid(), unit1Config.getDp3Value(), testTimestamp),
      createMeasurement(unit1Config.getCycleUid(), unit1Config.getCycleValue(), testTimestamp),
      createMeasurement(unit2Config.getDp1Uid(), unit2Config.getDp1Value(), testTimestamp),
      createMeasurement(unit2Config.getDp2Uid(), unit2Config.getDp2Value(), testTimestamp),
      createMeasurement(unit2Config.getDp3Uid(), unit2Config.getDp3Value(), testTimestamp),
      createMeasurement(unit2Config.getCycleUid(), unit2Config.getCycleValue(), testTimestamp)
    ));

    doAnswer(invocation -> {
      Runnable task = invocation.getArgument(0);
      task.run();
      return null;
    }).when(singleThreadExecutor).submit(any(Runnable.class));

    measurementChangeTracker.processAndTrackChanges(list);

    assertTrue(measurementChangeTrackerReflectionTestHelper.getUnitInitialDataLoaded().containsKey(unit1Config.getUnitName()));
    assertTrue(measurementChangeTrackerReflectionTestHelper.getUnitInitialDataLoaded().containsKey(unit2Config.getUnitName()));
  }

  @Test
  void testProcessAndTrackChanges_SpecialValue99999() {
    TestDataConfig config = createUnit1Config(Instant.now());

    Unit mockUnit = createMockUnit(config);
    when(unitCollection.getUnits()).thenReturn(List.of(mockUnit));
    when(unitCollection.getCommonFieldDto()).thenReturn(new CommonFieldDto());

    Instant firstTimestamp = testTimestamp;
    MeasurementList firstList = createMeasurementList(Arrays.asList(
      createMeasurement(config.getDp1Uid(), 99999.0, firstTimestamp),
      createMeasurement(config.getDp2Uid(), 2032.25, firstTimestamp),
      createMeasurement(config.getDp3Uid(), 4357.09, firstTimestamp),
      createMeasurement(config.getCycleUid(), 26319.0, firstTimestamp)
    ));

    doAnswer(invocation -> {
      Runnable task = invocation.getArgument(0);
      task.run();
      return null;
    }).when(singleThreadExecutor).submit(any(Runnable.class));

    measurementChangeTracker.processAndTrackChanges(firstList);

    Instant secondTimestamp = firstTimestamp.plusSeconds(2);
    TestDataConfig newConfig = config.setDp1Value(99999.0).setDp2Value(1234.0).setDp3Value(5679.0).setCycleValue(26320.0).setTimeStamp(secondTimestamp);

    MeasurementList secondList = createMeasurementList(Arrays.asList(
      createMeasurement(newConfig.getDp1Uid(), newConfig.getDp1Value(), secondTimestamp),
      createMeasurement(newConfig.getDp2Uid(), newConfig.getDp2Value(), secondTimestamp),
      createMeasurement(newConfig.getDp3Uid(), newConfig.getDp3Value(), secondTimestamp),
      createMeasurement(newConfig.getCycleUid(), newConfig.getCycleValue(), secondTimestamp)
    ));

    measurementChangeTracker.processAndTrackChanges(secondList);

    verify(singleThreadExecutor, times(1)).submit(runnableCaptor.capture());
    verify(dataReadyCallback, times(1)).onDataReady(any(StoreData.class), eq(newConfig.getUnitName()));
  }

  @Test
  void testCallbackThrowsException_LogsError() {
    TestDataConfig config = createUnit1Config(Instant.now());

    Unit mockUnit = createMockUnit(config);
    when(unitCollection.getUnits()).thenReturn(List.of(mockUnit));
    when(unitCollection.getCommonFieldDto()).thenReturn(new CommonFieldDto());
    Instant firstTimestamp = testTimestamp;

    MeasurementList list = createMeasurementList(Arrays.asList(
      createMeasurement(config.getDp1Uid(), 2706.18, firstTimestamp),
      createMeasurement(config.getDp2Uid(), 2032.25, firstTimestamp),
      createMeasurement(config.getDp3Uid(), 4357.09, firstTimestamp),
      createMeasurement(config.getCycleUid(), 26319.0, firstTimestamp)
    ));

    measurementChangeTracker.processAndTrackChanges(list);

    Instant secondTimestamp = firstTimestamp.plusSeconds(2);
    TestDataConfig newConfig = config.setDp1Value(2800.0).setDp2Value(2032.26).setDp3Value(4357.10).setCycleValue(26320.0).setTimeStamp(secondTimestamp);

    MeasurementList secondList = createMeasurementList(Arrays.asList(
      createMeasurement(config.getDp1Uid(), newConfig.getDp1Value(), secondTimestamp),
      createMeasurement(config.getDp2Uid(), newConfig.getDp2Value(), secondTimestamp),
      createMeasurement(config.getDp3Uid(), newConfig.getDp3Value(), secondTimestamp),
      createMeasurement(config.getCycleUid(), newConfig.getCycleValue(), secondTimestamp)
    ));

    doThrow(new RuntimeException("Test callback exception"))
      .when(dataReadyCallback).onDataReady(any(StoreData.class), anyString());

    doAnswer(invocation -> {
      Runnable task = invocation.getArgument(0);
      task.run();
      return null;
    }).when(singleThreadExecutor).submit(any(Runnable.class));

    assertDoesNotThrow(() -> measurementChangeTracker.processAndTrackChanges(secondList));

    verify(singleThreadExecutor, times(1)).submit(any(Runnable.class));
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
    Parameter param1 = new Parameter(s1, config.getDp1Uid());
    param1.setData(config.getDp1Value(), config.getTimestamp(), 0);
    parameters.add(param1);

    Parameter param2 = new Parameter(s2, config.getDp2Uid());
    param2.setData(config.getDp2Value(), config.getTimestamp(), 0);
    parameters.add(param2);

    Parameter param3 = new Parameter(s3, config.getDp3Uid());
    param3.setData(config.getDp3Value(), config.getTimestamp(), 0);
    parameters.add(param3);

    Parameter cycleParam = new Parameter(s4, config.getCycleUid());
    cycleParam.setData(config.getCycleValue(), config.getTimestamp(), 0);
    parameters.add(cycleParam);

    when(mockUnit.getParameters()).thenReturn(parameters);
    when(mockUnit.getTopologies()).thenReturn(new ArrayList<>());
    when(mockUnit.getElements()).thenReturn(new ArrayList<>());
    when(mockUnit.getInfluencingFactors()).thenReturn(new ArrayList<>());
    when(mockUnit.getRepairSchema()).thenReturn(null);

    mockUnits.put(config.getUnitName(), mockUnit);
    return mockUnit;
  }

  private Measurement createMeasurement(String uid, double value, Instant timestamp) {
    Measurement measurement = mock(Measurement.class);
    when(measurement.getUid()).thenReturn(uid);
    when(measurement.getValue()).thenReturn(value);
    when(measurement.getTimeStamp()).thenReturn(timestamp.toString());
    when(measurement.getQCode()).thenReturn(0);
    return measurement;
  }

  private MeasurementList createMeasurementList(List<Measurement> measurements) {
    MeasurementList list = new MeasurementList();
    for (Measurement measurement : measurements) {
      list.add(measurement);
    }
    return list;
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
