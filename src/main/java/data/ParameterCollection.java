package data;

import java.util.HashMap;
import java.util.Map;

// Коллекция параметров (аналог Items[j].Parameters)
public class ParameterCollection {
  private final Map<String, Parameter> parameters;

  public ParameterCollection() {
    this.parameters = new HashMap<>();
  }

  public void add(Parameter parameter) {
    parameters.put(parameter.getId(), parameter);
  }

  public Parameter get(String id) {
    return parameters.get(id);
  }

  // Аналог Items[j].Parameters.Data[k]
  public Iterable<Parameter> getAllParameters() {
    return parameters.values();
  }

  public boolean contains(String id) {
    return parameters.containsKey(id);
  }

  public int size() {
    return parameters.size();
  }
}
