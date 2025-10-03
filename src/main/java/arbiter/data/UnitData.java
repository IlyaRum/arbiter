package arbiter.data;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class UnitData {
  private final Unit unit;
  private final List<Parameter> parameters;

  public UnitData(Unit unit) {
    this.unit = unit;
    this.parameters = new ArrayList<>();
  }

  public UnitData(Unit unit, List<Parameter> parameters) {
    this.unit = unit;
    this.parameters = parameters;
  }

  public void addParameter(Parameter parameter) {
    parameters.add(parameter);
  }

  public Unit getUnit() {
    return unit;
  }

  public List<Parameter> getParameters() {
    return parameters;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", UnitData.class.getSimpleName() + "[", "]")
      .add("parameters=" + parameters)
      .add("unit=" + unit)
      .toString();
  }
}
