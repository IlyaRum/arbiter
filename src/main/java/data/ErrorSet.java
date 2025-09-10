package data;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


//набор сработавших критериев перехода на ПУР
public class ErrorSet {
  private String name;
  private boolean changed;
  private boolean added;
  private boolean deleted;
  private Map<String, Integer> errors ;

  public ErrorSet(String name) {
    this.name = name;
    this.changed = false;
    this.added = false;
    this.deleted = false;
    this.errors = new ConcurrentHashMap<>();
  }

  public void plus(String errorName) {
    if (!errors.containsKey(errorName)) {
      errors.put(errorName, 1);
      changed = true;
      added = true;
      // Логирование
    }
  }

  public boolean isEmpty() {
    return errors.isEmpty();
  }
}
