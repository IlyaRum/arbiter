package arbiter.data;

import java.util.StringJoiner;

/**
 * топология
 */

public class Topology {
  private String id;
  private String name;
  protected double value;

  public Topology(String id, String name) {
    this.id = id;
    this.name = name;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
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

  @Override
  public String toString() {
    return new StringJoiner(", ", Topology.class.getSimpleName() + "[", "]")
      .add("id='" + id + "'")
      .add("name='" + name + "'")
      .add("value=" + value)
      .toString();
  }
}
