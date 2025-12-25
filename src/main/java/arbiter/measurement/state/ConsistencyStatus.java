package arbiter.measurement.state;

/**
 * Статус согласованности всех UID'ов и одинаковый timestamp
 */
public class ConsistencyStatus {
  final boolean allTargetsHaveData;
  final boolean allTimestampsMatch;

  public ConsistencyStatus(boolean allTargetsHaveData, boolean allTimestampsMatch) {
    this.allTargetsHaveData = allTargetsHaveData;
    this.allTimestampsMatch = allTimestampsMatch;
  }

  public boolean isAllTargetsHaveData() {
    return allTargetsHaveData;
  }

  public boolean isAllTimestampsMatch() {
    return allTimestampsMatch;
  }
}
