package arbiter.data;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class StoreData {
  private final List<Parameter> parameters;

  public StoreData() {
    this.parameters = new ArrayList<>();
  }

  public void add(Parameter parameter) {
    parameters.add(parameter);
  }

  public int size() {
    return parameters.size();
  }

  public List<Parameter> getParameters() {
    return new ArrayList<>(parameters);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", StoreData.class.getSimpleName() + "[", "]")
      .add("parameters=" + parameters)
      .toString();
  }
}
