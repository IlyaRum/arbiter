package arbiter.data;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * Ремонтная схема
 */

public class RepairSchemaData {
  private Integer group;
  private List<Composition> values = new ArrayList<>();

  private List<String> UIDs = new ArrayList<>();

  public RepairSchemaData() {
  }

  public RepairSchemaData(Integer group, List<Composition> values) {
    this.group = group;
    this.values = values;
  }

  public Integer getGroup() {
    return group;
  }

  public void setGroup(Integer group) {
    this.group = group;
  }

  public List<Composition> getValues() {
    return values;
  }

  public void setValues(List<Composition> values) {
    this.values = values;
  }

  public void setComposition(Composition composition) {
    this.values.add(composition);
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

  @Override
  public String toString() {
    return new StringJoiner(", ", RepairSchemaData.class.getSimpleName() + "[", "]")
      .add("group=" + getGroup())
      .add("values=" + getValues())
      .toString();
  }
}
