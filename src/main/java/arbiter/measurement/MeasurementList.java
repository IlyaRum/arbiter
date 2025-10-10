package arbiter.measurement;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class MeasurementList {
  private final List<Measurement> measurements;

  public MeasurementList() {
    this.measurements = new ArrayList<>();
  }

  public void add(Measurement measurement) {
    measurements.add(measurement);
  }

  public void add(JsonNode jsonObject) {
    measurements.add(new Measurement(jsonObject));
  }

  public int size() {
    return measurements.size();
  }

  public Measurement get(int index) {
    return measurements.get(index);
  }

  public List<Measurement> getMeasurements() {
    return new ArrayList<>(measurements);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", MeasurementList.class.getSimpleName() + "[", "]")
      .add("measurements=" + measurements)
      .toString();
  }
}
