package arbiter.data;

import java.time.Instant;
import java.util.StringJoiner;

/**
 * Состав
 */
public class Composition {
  private String id;
  private String name;
  protected double value;
  protected Instant time;
  private int qCode;
  private int min;
  private int max;

  public Composition(String id, String name) {
    this.id = id;
    this.name = name;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public double getValue() {
    return value;
  }

  public void setValue(double value) {
    this.value = value;
  }

  public Instant getTime() {
    return time;
  }

  public void setTime(Instant time) {
    this.time = time;
  }

  public int getqCode() {
    return qCode;
  }

  public void setqCode(int qCode) {
    this.qCode = qCode;
  }

  public int getMin() {
    return min;
  }

  public void setMin(int min) {
    this.min = min;
  }

  public int getMax() {
    return max;
  }

  public void setMax(int max) {
    this.max = max;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", Composition.class.getSimpleName() + "[", "]")
      .add("id='" + getId() + "'")
      .add("name='" + getName() + "'")
      .add("value=" + getValue())
      .add("time=" + getTime())
      .add("qCode=" + getqCode())
      .add("min=" + getMin())
      .add("max=" + getMax())
      .toString();
  }
}
