package data;

import java.time.Instant;

public class Parameter extends Result {
  private boolean assigned;
  private double oldValue;
  private int qCode;
  private double min;
  private double max;
  private boolean selfTest;

  public Parameter(Unit unit, String name, String id) {
    super(unit, name, id);

    if (id != null && id.length() == 36) {
      unit.getCollection().addID(id);
    }
  }

  public boolean checkLimits() {
    if (selfTest) {
      return getValue() == 99999 || (getValue() >= min && getValue() <= max);
    } else {
      return (qCode & 0x00000004) == 0;
    }
  }

  @Override
  public void setValue(double value, Instant time) {
    this.assigned = true;
    this.oldValue = getValue();
    super.setValue(value, time);
  }

  public int getOldInt() {
    return (int) Math.round(oldValue);
  }

  public boolean isAssigned() {
    return assigned;
  }
}
