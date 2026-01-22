package arbiter.helper;

import arbiter.data.model.*;
import arbiter.measurement.MeasurementChangeTracker;
import arbiter.measurement.Measurement;
import arbiter.measurement.state.ConsistencyStatus;
import arbiter.measurement.state.UnitState;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Класс для тестирования приватных методов
 */
public class MeasurementChangeTrackerReflectionTestHelper {

  private final MeasurementChangeTracker measurementChangeTracker;

  public MeasurementChangeTrackerReflectionTestHelper(MeasurementChangeTracker measurementChangeTracker) {
    this.measurementChangeTracker = measurementChangeTracker;
  }

  public Map<String, Measurement> invokeCreateMeasurementsMap(List<Measurement> measurements) {
    try {
      java.lang.reflect.Method method = MeasurementChangeTracker.class.getDeclaredMethod("createMeasurementsMap", List.class);
      method.setAccessible(true);
      return (Map<String, Measurement>) method.invoke(measurementChangeTracker, measurements);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void invokeSendTrackedChanges(String unitId, UnitState unitState) {
    try {
      java.lang.reflect.Method method = MeasurementChangeTracker.class.getDeclaredMethod(
        "sendTrackedChanges", String.class, UnitState.class);
      method.setAccessible(true);
      method.invoke(measurementChangeTracker, unitId, unitState);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public ConsistencyStatus invokeCheckAllTargetsHaveConsistentData(String unitId, Set<String> targetUids,
                                                                   Map<String, Measurement> dataBuffer, Instant referenceTimestamp) {
    try {
      java.lang.reflect.Method method = MeasurementChangeTracker.class.getDeclaredMethod(
        "checkAllTargetsHaveConsistentData", String.class, Set.class, Map.class, Instant.class);
      method.setAccessible(true);
      return (ConsistencyStatus) method.invoke(measurementChangeTracker, unitId, targetUids, dataBuffer, referenceTimestamp);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public Map<String, Map<String, Measurement>> getUnitDataBuffers() {
    try {
      java.lang.reflect.Field field = MeasurementChangeTracker.class.getDeclaredField("unitDataBuffers");
      field.setAccessible(true);
      return (Map<String, Map<String, Measurement>>) field.get(measurementChangeTracker);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public Map<String, Boolean> getUnitInitialDataLoaded() {
    try {
      java.lang.reflect.Field field = MeasurementChangeTracker.class.getDeclaredField("unitInitialDataLoaded");
      field.setAccessible(true);
      return (Map<String, Boolean>) field.get(measurementChangeTracker);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public Map<String, Map<String, Double>> getUnitPreviousParameterValues() {
    try {
      java.lang.reflect.Field field = MeasurementChangeTracker.class.getDeclaredField("unitPreviousParameterValues");
      field.setAccessible(true);
      return (Map<String, Map<String, Double>>) field.get(measurementChangeTracker);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public Map<String, Map<String, Parameter>> getUnitTrackedChanges() {
    try {
      java.lang.reflect.Field field = MeasurementChangeTracker.class.getDeclaredField("unitTrackedParameterChanges");
      field.setAccessible(true);
      return (Map<String, Map<String, Parameter>>) field.get(measurementChangeTracker);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public Map<String, Map<String, Topology>> getUnitTrackedTopologyChanges() {
    try {
      java.lang.reflect.Field field = MeasurementChangeTracker.class.getDeclaredField("unitTrackedTopologyChanges");
      field.setAccessible(true);
      return (Map<String, Map<String, Topology>>) field.get(measurementChangeTracker);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public Map<String, Map<String, Element>> getUnitTrackedElementChanges() {
    try {
      java.lang.reflect.Field field = MeasurementChangeTracker.class.getDeclaredField("unitTrackedElementChanges");
      field.setAccessible(true);
      return (Map<String, Map<String, Element>>) field.get(measurementChangeTracker);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public Map<String, Map<String, InfluencingFactor>> getUnitTrackedFactorChanges() {
    try {
      java.lang.reflect.Field field = MeasurementChangeTracker.class.getDeclaredField("unitTrackedFactorChanges");
      field.setAccessible(true);
      return (Map<String, Map<String, InfluencingFactor>>) field.get(measurementChangeTracker);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public Map<String, Map<String, Composition>> getUnitTrackedRepairChanges() {
    try {
      java.lang.reflect.Field field = MeasurementChangeTracker.class.getDeclaredField("unitTrackedRepairChanges");
      field.setAccessible(true);
      return (Map<String, Map<String, Composition>>) field.get(measurementChangeTracker);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public Map<String, Instant> getUnitCurrentTimestamps() {
    try {
      java.lang.reflect.Field field = MeasurementChangeTracker.class.getDeclaredField("unitCurrentTimestamps");
      field.setAccessible(true);
      return (Map<String, Instant>) field.get(measurementChangeTracker);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
