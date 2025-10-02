package arbiter.data;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

public class Parameter extends Result {
  private boolean assigned;
  private double oldValue;
  private int qCode;
  private double min;
  private double max;
  private boolean selfTest;
  private List<String> UIDs = new ArrayList<>();

  public Parameter(String name, String id) {
    super(name, id);
    addID(id);
  }

  public void addID(final String id) {
    if (id != null && id.length() == 36) {
      if (!UIDs.contains(id)) {
        UIDs.add(id);
      }
    }
  }

  public boolean checkLimits() {
    if (selfTest) {
      return getValue() == 99999 || (getValue() >= min && getValue() <= max);
    } else {
      return (qCode & 0x00000004) == 0;
    }
  }

  @Override
  public void setValue(double value, Instant time) {
    this.assigned = true;
    this.oldValue = getValue();
    super.setValue(value, time);
  }

  // Для сравнения (аналог P.Value <> Data.Value или P.Time <> Data.Time)

  public boolean isDataDifferent(double newValue, Instant newTime) {
    return !assigned ||
      Double.compare(value, newValue) != 0 ||
      !Objects.equals(time, newTime);
  }
  // Аналог P.SetData(Data.Value, Data.Time, Data.QCode)

  public void setData(double value, Instant time, int qCode) {
    this.value = value;
    this.time = time;
    this.qCode = qCode;
    this.assigned = true;
  }
  public double getMax() {
    return max;
  }

  public double getMin() {
    return min;
  }

  public int getQCode() {
    return qCode;
  }

  @JsonIgnore
  public int getOldInt() {
    return (int) Math.round(oldValue);
  }

  @JsonIgnore
  public boolean isAssigned() {
    return assigned;
  }

  @JsonIgnore
  public double getOldValue() {
    return oldValue;
  }

  @JsonIgnore
  public boolean isSelfTest() {
    return selfTest;
  }

  @JsonIgnore
  public List<String> getUIDs() {
    return UIDs;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", Parameter.class.getSimpleName() + "[", "]")
      .add("name=" + getName())
      .add("id=" + getId())
      .add("value=" + getValue())
      .add("time=" + getTime())
      //.add("Unit="+ getUnit())
      .add("assigned=" + assigned)
      .add("max=" + max)
      .add("min=" + min)
      .add("oldValue=" + oldValue)
      .add("qCode=" + qCode)
      .add("selfTest=" + selfTest)
      .add("time=" + time)
      .add("value=" + value)
      .toString();
  }
}
