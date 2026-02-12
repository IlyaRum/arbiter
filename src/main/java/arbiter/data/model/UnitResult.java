package arbiter.data.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Поле результат
 */
public class UnitResult {
  private String uid;
  private String name;
  protected double value;
  protected Instant time;

  private List<String> UIDs = new ArrayList<>();

  public UnitResult(String name, String uid) {
    this.uid = uid;
    this.name = name;
    addID(uid);
  }

  public String getUid() {
    return uid;
  }

  public void setUid(String uid) {
    this.uid = uid;
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

  public void addID(final String id) {
    if (id != null && id.length() == 36) {
      if (!UIDs.contains(id)) {
        UIDs.add(id);
      }
    }
  }

  @JsonIgnore
  public List<String> getUIDs() {
    return UIDs;
  }

  public boolean isDataDifferent(double newValue, Instant newTime) {
    return Double.compare(value, newValue) != 0 ||
      !Objects.equals(time, newTime);
  }

  public void setData(double value, Instant time, int qCode) {
    this.value = value;
    this.time = time;
  }
}
