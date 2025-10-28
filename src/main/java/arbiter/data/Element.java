package arbiter.data;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * ТС элементов
 */
public class Element {
  private String id;
  private String name;
  protected double value;
  private int qCode;
  private int min;
  private int max;
  protected Instant time;

  private List<String> UIDs = new ArrayList<>();

  public Element(String id, String name) {
    this.id = id;
    this.name = name;
    addID(id);
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

  @JsonIgnore
  public List<String> getUIDs() {
    return UIDs;
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

  public Instant getTime() {
    return time;
  }

  public void setTime(Instant time) {
    this.time = time;
  }
}
