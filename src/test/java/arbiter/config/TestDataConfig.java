package arbiter.config;

import java.time.Instant;
import java.util.Set;

public class TestDataConfig {

  private final String unitName;
  private final String dp1Uid;
  private final String dp2Uid;
  private final String dp3Uid;
  private final String cycleUid;
  private final Set<String> targetUids;
  private Instant timestamp;
  private double dp1Value = 0.0;
  private double dp2Value = 0.0;
  private double dp3Value = 0.0;
  private double cycleValue = 0.0;

  public TestDataConfig(String unitName, String dp1Uid, String dp2Uid, String dp3Uid, String cycleUid, Set<String> targetUids, Instant timestamp) {
    this.unitName = unitName;
    this.dp1Uid = dp1Uid;
    this.dp2Uid = dp2Uid;
    this.dp3Uid = dp3Uid;
    this.cycleUid = cycleUid;
    this.targetUids = targetUids;
    this.timestamp = timestamp;
  }

  public String getUnitName() {
    return unitName;
  }

  public String getDp1Uid() {
    return dp1Uid;
  }

  public String getDp2Uid() {
    return dp2Uid;
  }

  public String getDp3Uid() {
    return dp3Uid;
  }

  public String getCycleUid() {
    return cycleUid;
  }

  public Set<String> getTargetUids() {
    return targetUids;
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  public double getDp1Value() {
    return dp1Value;
  }

  public double getDp2Value() {
    return dp2Value;
  }

  public double getDp3Value() {
    return dp3Value;
  }

  public double getCycleValue() {
    return cycleValue;
  }

  public TestDataConfig setDp1Value(double value) {
    this.dp1Value = value;
    return this;
  }

  public TestDataConfig setDp2Value(double value) {
    this.dp2Value = value;
    return this;
  }

  public TestDataConfig setDp3Value(double value) {
    this.dp3Value = value;
    return this;
  }

  public TestDataConfig setCycleValue(double value) {
    this.cycleValue = value;
    return this;
  }

  public TestDataConfig setTimeStamp(Instant value) {
    this.timestamp = value;
    return this;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String unitName;
    private String dp1Uid;
    private String dp2Uid;
    private String dp3Uid;
    private String cycleUid;
    private Set<String> targetUids;
    private Instant timestamp;

    public Builder unitName(String unitName) {
      this.unitName = unitName;
      return this;
    }

    public Builder dp1Uid(String dp1Uid) {
      this.dp1Uid = dp1Uid;
      return this;
    }

    public Builder dp2Uid(String dp2Uid) {
      this.dp2Uid = dp2Uid;
      return this;
    }

    public Builder dp3Uid(String dp3Uid) {
      this.dp3Uid = dp3Uid;
      return this;
    }

    public Builder cycleUid(String cycleUid) {
      this.cycleUid = cycleUid;
      return this;
    }

    public Builder targetUids(Set<String> targetUids) {
      this.targetUids = targetUids;
      return this;
    }

    public Builder timestamp(Instant timestamp) {
      this.timestamp = timestamp;
      return this;
    }

    public TestDataConfig build() {
      return new TestDataConfig(unitName, dp1Uid, dp2Uid, dp3Uid, cycleUid, targetUids, timestamp);
    }
  }
}
