package arbiter.measurement.state;

import arbiter.data.model.Element;
import arbiter.data.model.InfluencingFactor;
import arbiter.data.model.Parameter;
import arbiter.data.model.Topology;

import java.util.Map;

/**
 * Состояние сечения
 */
public class UnitState {
  final Map<String, Parameter> trackedChanges;
  final Map<String, Topology> trackedTopologyChanges;
  final Map<String, Element> trackedElementChanges;
  final Map<String, InfluencingFactor> trackedInfluencingFactorChanges;

  public UnitState(Map<String, Parameter> trackedChanges,
            Map<String, Topology> trackedTopologyChanges,
            Map<String, Element> trackedElementChanges,
            Map<String, InfluencingFactor> trackedInfluencingFactorChanges) {
    this.trackedChanges = trackedChanges;
    this.trackedTopologyChanges = trackedTopologyChanges;
    this.trackedElementChanges = trackedElementChanges;
    this.trackedInfluencingFactorChanges = trackedInfluencingFactorChanges;
  }

  public Map<String, Parameter> getTrackedChanges() {
    return trackedChanges;
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
}
