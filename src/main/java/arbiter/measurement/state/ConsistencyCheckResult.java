package arbiter.measurement.state;

import java.time.Instant;

/**
 * Результаты проверки согласованности
 */
public class ConsistencyCheckResult {
  final boolean canCheckConsistency;
  final Instant referenceTimestamp;

  public ConsistencyCheckResult(boolean canCheckConsistency, Instant referenceTimestamp) {
    this.canCheckConsistency = canCheckConsistency;
    this.referenceTimestamp = referenceTimestamp;
  }

  public boolean isCanCheckConsistency() {
    return canCheckConsistency;
  }

  public Instant getReferenceTimestamp() {
    return referenceTimestamp;
  }
}
