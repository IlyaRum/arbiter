package arbiter.data;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 *  Телесигналы
 */

public class RepairGroupValue {
  private Integer group;
  private String operation;
  private List<Composition> values = new ArrayList<>();



  public RepairGroupValue() {
  }

  public RepairGroupValue(Integer group, List<Composition> values) {
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

  public String getOperation() {
    return operation;
  }

  public void setOperation(String operation) {
    this.operation = operation;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", RepairGroupValue.class.getSimpleName() + "[", "]")
      .add("group=" + group)
      .add("operation='" + operation + "'")
      .add("values=" + values)
      .toString();
  }
}
