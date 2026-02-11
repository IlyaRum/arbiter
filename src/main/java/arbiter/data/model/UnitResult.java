package arbiter.data.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;

/**
 * Поле результат
 */
public class UnitResult {
  private String uid;
  private String name;

  private List<String> UIDs = new ArrayList<>();

  public UnitResult(String name, String uid) {
    this.uid = uid;
    this.name = name;
    addID(uid);
  }

  public String getUid() {
    return uid;
  }

  public void setUid(String uid) {
    this.uid = uid;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void addID(final String id) {
    if (id != null && id.length() == 36) {
      if (!UIDs.contains(id)) {
        UIDs.add(id);
      }
    }
  }

  @JsonIgnore
  public List<String> getUIDs() {
    return UIDs;
  }
}
