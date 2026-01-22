package arbiter.measurement.state;

import arbiter.data.model.*;

import java.util.Map;

/**
 * Состояние сечения
 */
public class UnitState {
  final Map<String, Parameter> trackedParameterChanges;
  final Map<String, Topology> trackedTopologyChanges;
  final Map<String, Element> trackedElementChanges;
  final Map<String, InfluencingFactor> trackedInfluencingFactorChanges;
  final Map<String, Composition> trackedTrackedRepairChanges;

  public UnitState(Map<String, Parameter> trackedChanges,
            Map<String, Topology> trackedTopologyChanges,
            Map<String, Element> trackedElementChanges,
            Map<String, InfluencingFactor> trackedInfluencingFactorChanges,
                   Map<String, Composition> trackedTrackedRepairChanges) {
    this.trackedParameterChanges = trackedChanges;
    this.trackedTopologyChanges = trackedTopologyChanges;
    this.trackedElementChanges = trackedElementChanges;
    this.trackedInfluencingFactorChanges = trackedInfluencingFactorChanges;
    this.trackedTrackedRepairChanges = trackedTrackedRepairChanges;
  }

  public Map<String, Parameter> getTrackedParameterChanges() {
    return trackedParameterChanges;
  }

  public Map<String, Topology> getTrackedTopologyChanges() {
    return trackedTopologyChanges;
  }

  public Map<String, Element> getTrackedElementChanges() {
    return trackedElementChanges;
  }

  public Map<String, InfluencingFactor> getTrackedInfluencingFactorChanges() {
    return trackedInfluencingFactorChanges;
  }
  public Map<String, Composition> getTrackedRepairChanges() {
    return trackedTrackedRepairChanges;
  }
}
