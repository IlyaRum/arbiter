package arbiter.data;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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

  private List<String> UIDs = new ArrayList<>();

  public Composition(String id, String name) {
    this.id = id;
    this.name = name;
    addID(id);
  }

  @JsonIgnore
  public List<String> getUIDs() {
    return UIDs;
  }

  public void addID(final String id) {
    if (id != null && id.length() == 36) {
      if (!UIDs.contains(id)) {
        UIDs.add(id);
      }
    }
  }


  public boolean isDataDifferent(double newValue, Instant newTime) {
    return Double.compare(value, newValue) != 0 ||
      !Objects.equals(time, newTime);
  }

  public void setData(double value, Instant time, int qCode) {
    this.value = value;
    this.time = time;
    this.qCode = qCode;
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

  @Override
  public String toString() {
    return new StringJoiner(", ", Composition.class.getSimpleName() + "[", "]")
      .add("id='" + getId() + "'")
      .add("name='" + getName() + "'")
      .add("value=" + getValue())
      .add("time=" + getTime())
      .add("qCode=" + getqCode())
      .toString();
  }
}
