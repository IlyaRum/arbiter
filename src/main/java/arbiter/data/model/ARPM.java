package arbiter.data.model;

import java.util.List;
import java.util.StringJoiner;

/**
 * Блок АРПМ
 */

public class ARPM {
  private String name;
  private List<ParameterArpm> parameterArpm;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<ParameterArpm> getParameterArpm() {
    return parameterArpm;
  }

  public void setParameterArpm(List<ParameterArpm> parameterArpm) {
    this.parameterArpm = parameterArpm;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", ARPM.class.getSimpleName() + "[", "]")
      .add("name='" + name + "'")
      .add("parameterArpm=" + parameterArpm)
      .toString();
  }
}
