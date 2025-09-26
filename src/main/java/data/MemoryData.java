package data;

import java.time.Instant;
import java.util.Objects;

public class MemoryData {
  private final String id;
  private final double value;
  private final Instant time;
  private final int qCode;

  public MemoryData(String id, double value, Instant time, int qCode) {
    this.id = id;
    this.value = value;
    this.time = time;
    this.qCode = qCode;
  }

  // Getters
  public String getId() { return id; }
  public double getValue() { return value; }
  public Instant getTime() { return time; }
  public int getQCode() { return qCode; }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MemoryData that = (MemoryData) o;
    return Double.compare(that.value, value) == 0 &&
      qCode == that.qCode &&
      Objects.equals(id, that.id) &&
      Objects.equals(time, that.time);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, value, time, qCode);
  }
}
