package arbiter.data;

/**
 * Влияющие ТИ
 */

public class InfluencingFactor {
  private String id;
  private String name;
  protected double value;

  public InfluencingFactor(String id, String name) {
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
}
