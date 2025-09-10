package measurement;

import io.vertx.core.json.JsonObject;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class Measurement {

  public enum MeasurementType {
    NUMBER,
    STRING
  }

  private MeasurementType type;
  private String uid;
  private String timeStamp;
  private String timeStamp2;
  private int qCode;
  private double value;
  private String strValue;
  private boolean changed;



  // Конструктор из JSON
  public Measurement(JsonObject obj) {
    JsonObject data = obj.containsKey("value") && obj.getValue("value") instanceof JsonObject
      ? obj.getJsonObject("value")
      : obj;

    this.uid = data.getString("uid");

    if (!data.containsKey("timeStamp")) {
      this.timeStamp = "1969-12-31T21:00:00.000Z";
      this.timeStamp2 = "1969-12-31T21:00:00.000Z";
    } else {
      this.timeStamp = data.getString("timeStamp");
      this.timeStamp2 = data.getString("timeStamp2");
    }

    this.qCode = data.getInteger("qCode", 0x80000002);

    Object valueObj = data.getValue("value");
    if (valueObj instanceof String) {
      this.type = MeasurementType.STRING;
      this.strValue = (String) valueObj;
    } else {
      this.type = MeasurementType.NUMBER;
      this.value = data.getDouble("value");
    }

    this.changed = false;
  }

  // Конструктор для числового значения
  public Measurement(String uid, ZonedDateTime dateTime, double value) {
    init(uid, dateTime, 0x80000002);
    this.value = value;
    this.type = MeasurementType.NUMBER;
  }

  // Конструктор для строкового значения
  public Measurement(String uid, ZonedDateTime dateTime, String value) {
    init(uid, dateTime, 0x80000002);
    this.strValue = value;
    this.type = MeasurementType.STRING;
  }

  private void init(String uid, ZonedDateTime dateTime, int qCode) {
    this.uid = uid;
    if (dateTime == null) {
      this.timeStamp = "";
    } else {
      this.timeStamp = toUTC(dateTime);
    }
    this.timeStamp2 = this.timeStamp;
    this.qCode = qCode;
    this.changed = false;
  }

  // Методы для преобразования времени
  private String toUTC(ZonedDateTime dateTime) {
    return dateTime.withZoneSameInstant(ZoneId.of("UTC"))
      .format(DateTimeFormatter.ISO_INSTANT);
  }

  private ZonedDateTime fromUTC(String utcString) {
    return Instant.parse(utcString).atZone(ZoneId.systemDefault());
  }

  public void assign(Measurement other) {
    this.type = other.getType();
    this.uid = other.getUid();
    this.timeStamp = other.getTimeStamp();
    this.timeStamp2 = other.getTimeStamp2();
    this.qCode = other.getQCode();
    this.value = other.getValue();
    this.strValue = other.getStrValue();
    this.changed = true;
  }

  // Геттеры
  public MeasurementType getType() { return type; }
  public String getUid() { return uid; }
  public String getTimeStamp() { return timeStamp; }
  public String getTimeStamp2() { return timeStamp2; }
  public int getQCode() { return qCode; }
  public double getValue() { return value; }
  public String getStrValue() { return strValue; }
  public boolean isChanged() { return changed; }

  public ZonedDateTime getTime() {
    return fromUTC(timeStamp);
  }

  public ZonedDateTime getTime2() {
    return fromUTC(timeStamp2);
  }

  // Сеттер для changed
  public void setChanged(boolean changed) {
    this.changed = changed;
  }

  public JsonObject toJson() {
    JsonObject result = new JsonObject();
    result.put("uid", uid);

    if (timeStamp == null || timeStamp.isEmpty()) {
      String currentTime = toUTC(ZonedDateTime.now());
      result.put("timeStamp", currentTime);
      result.put("timeStamp2", currentTime);
    } else {
      result.put("timeStamp", timeStamp);
      result.put("timeStamp2", timeStamp2);
    }

    result.put("qCode", qCode);

    if (type == MeasurementType.NUMBER) {
      result.put("value", value);
    } else {
      result.put("value", strValue);
    }

    return result;
  }
}
