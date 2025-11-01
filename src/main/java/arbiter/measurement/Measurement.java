package arbiter.measurement;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.StringJoiner;


/** пример измерений приходящих с СК-11
* {"uid":"4ebe8a8e-764d-43a1-bd8d-a5c1e0fb350a",
* "timeStamp":"2025-09-29T14:28:02.105Z",
* "timeStamp2":"2025-09-29T14:28:02.101Z",
* "qCode":1879048194,
* "value":3425.2886352539062}
**/


/**
 *  Измерения
 */
public class Measurement {
  public enum MeasurementType {
    STRING, NUMBER
  }

  private final String uid;
  private final String timeStamp;
  private final String timeStamp2;
  private final int qCode;
  private final MeasurementType type;
  private final String strValue;
  private final double value;
  private final boolean changed;

  public Measurement(JsonNode obj) {
    JsonNode data;

    if (obj.has("value") && obj.get("value").isObject()) {
      data = obj.get("value");
    } else {
      data = obj;
    }

    this.uid = data.get("uid").asText();

    if (!data.has("timeStamp")) {
      this.timeStamp = "1969-12-31T21:00:00.000Z";
      this.timeStamp2 = "1969-12-31T21:00:00.000Z";
    } else {
      this.timeStamp = data.get("timeStamp").asText();
      this.timeStamp2 = data.get("timeStamp2").asText();
    }

    this.qCode = data.get("qCode").asInt();

    if (data.has("value") && data.get("value").isTextual()) {
      this.type = MeasurementType.STRING;
      this.strValue = data.get("value").asText();
      this.value = 0.0;
    } else {
      this.type = MeasurementType.NUMBER;
      this.value = data.get("value").asDouble();
      this.strValue = null;
    }

    this.changed = false;
  }

  // Getters
  public String getUid() { return uid; }
  public String getTimeStamp() { return timeStamp; }
  public String getTimeStamp2() { return timeStamp2; }
  public int getQCode() { return qCode; }
  public MeasurementType getType() { return type; }
  public String getStrValue() { return strValue; }
  public double getValue() { return value; }
  public boolean isChanged() { return changed; }

//  public Instant getTimeStampAsInstant() {
//    return Instant.from(DateTimeFormatter.ISO_INSTANT.parse(timeStamp));
//  }

  @Override
  public String toString() {
    return new StringJoiner(", ", Measurement.class.getSimpleName() + "[", "]")
      .add("uid='" + uid + "'")
      .add("timeStamp='" + timeStamp + "'")
      .add("timeStamp2='" + timeStamp2 + "'")
      .add("qCode=" + qCode)
      .add("type=" + type)
      .add("strValue='" + strValue + "'")
      .add("value=" + value)
      .add("changed=" + changed)
      .toString();
  }
}
