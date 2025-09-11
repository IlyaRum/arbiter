package data;

import measurement.Measurement;

import java.time.Instant;
import java.time.ZonedDateTime;

public class Result {
  private Unit unit;
  private String name;
  private String id;
  private double value;
  private Instant time;

  public Result(Unit unit, String name, String id) {
    this.unit = unit;
    this.name = name;
    this.id = id != null ? id.toLowerCase() : null;
    this.value = 0;
    this.time = Instant.now();
  }

  public boolean writable() {
    return id != null && id.length() == 36;
  }

  public void setValue(double value, Instant time) {
    this.value = value;
    this.time = time;
    if (writable() && unit.getCollection().isWriteEnable()) {
      unit.getCollection().addToWriteBuffer(new Measurement(id, ZonedDateTime.now(), value));
    }
  }

  public int getInt() {
    return (int) Math.round(value);
  }

  public String getId() {
    return id;
  }

  public Instant getTime() {
    return time;
  }

  public String getName() {
    return name;
  }

  public double getValue() {
    return value;
  }
}
