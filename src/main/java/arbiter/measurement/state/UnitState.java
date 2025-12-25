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
  final Map<String, Parameter> accumulatedChanges;
  final Map<String, Topology> accumulatedTopologyChanges;
  final Map<String, Element> accumulatedElementChanges;
  final Map<String, InfluencingFactor> accumulatedInfluencingFactorChanges;

  public UnitState(Map<String, Parameter> accumulatedChanges,
            Map<String, Topology> accumulatedTopologyChanges,
            Map<String, Element> accumulatedElementChanges,
            Map<String, InfluencingFactor> accumulatedInfluencingFactorChanges) {
    this.accumulatedChanges = accumulatedChanges;
    this.accumulatedTopologyChanges = accumulatedTopologyChanges;
    this.accumulatedElementChanges = accumulatedElementChanges;
    this.accumulatedInfluencingFactorChanges = accumulatedInfluencingFactorChanges;
  }

  public Map<String, Parameter> getAccumulatedChanges() {
    return accumulatedChanges;
  }

  public Map<String, Topology> getAccumulatedTopologyChanges() {
    return accumulatedTopologyChanges;
  }

  public Map<String, Element> getAccumulatedElementChanges() {
    return accumulatedElementChanges;
  }

  public Map<String, InfluencingFactor> getAccumulatedInfluencingFactorChanges() {
    return accumulatedInfluencingFactorChanges;
  }
}
