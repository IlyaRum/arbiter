package arbiter.data;

import arbiter.constants.ParameterMappingConstants;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Исходные данные
 */

public class Parameter{
  private String id;
  private String name;
  private String mappingFieldName;
  protected double value;
  protected Instant time;
  private boolean assigned;
  private double oldValue;
  private int qCode;
  private int min;
  private int max;
//  private boolean selfTest;
  private List<String> UIDs = new ArrayList<>();

  public Parameter(String name, String id) {
    this.name = name;
    this.id = id != null ? id.toLowerCase() : null;
    this.value = 0;
    this.time = Instant.now();
    setMappingFieldNameFromName(name);
    addID(id);
  }

  public Parameter(String name, String id, int min, int max) {
    this(name, id);
    this.min = min;
    this.max = max;
  }

  public void addID(final String id) {
    if (id != null && id.length() == 36) {
      if (!UIDs.contains(id)) {
        UIDs.add(id);
      }
    }
  }

//  public boolean checkLimits() {
//    if (selfTest) {
//      return getValue() == 99999 || (getValue() >= min && getValue() <= max);
//    } else {
//      return (qCode & 0x00000004) == 0;
//    }
//  }

  public void setValue(double value, Instant time) {
    this.assigned = true;
    this.oldValue = getValue();
    this.value = value;
    this.time = time;
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

  private void setMappingFieldNameFromName(String name) {
    if (this.name != null) {
      this.mappingFieldName = ParameterMappingConstants.PARAMETER_NAME_TO_FIELD_MAPPING.get(name.trim());
    } else {
      this.mappingFieldName = null;
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public int getMax() {
    return max;
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public int getMin() {
    return min;
  }

  public int getQCode() {
    return qCode;
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

//  public int getqCode() {
//    return qCode;
//  }

  public Instant getTime() {
    return time;
  }

  public double getValue() {
    return value;
  }

  public String getMappingFieldName() {
    return mappingFieldName;
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

//  @JsonIgnore
//  public boolean isSelfTest() {
//    return selfTest;
//  }

  @JsonIgnore
  public List<String> getUIDs() {
    return UIDs;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", Parameter.class.getSimpleName() + "[", "]")
      .add("id=" + getId())
      .add("name=" + getName())
      .add("mappingFieldName=" + getMappingFieldName())
      .add("value=" + getValue())
      .add("time=" + getTime())
      .add("max=" + getMax())
      .add("min=" + getMin())
      .add("qCode=" + getQCode())
//      .add("Unit="+ getUnit())
//      .add("assigned=" + assigned)
//      .add("oldValue=" + oldValue)
//      .add("selfTest=" + selfTest)
      .toString();
  }
}
