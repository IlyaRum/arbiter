package arbiter.data;

import arbiter.data.serialize.ParametersMapSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class UnitData {
  private final Unit unit;
  @JsonSerialize(using = ParametersMapSerializer.class)
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
      .add("unit=" + unit)
      .add("parameters=" + parameters)
      .toString();
  }
}
