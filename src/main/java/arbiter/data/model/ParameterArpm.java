package arbiter.data.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;

/**
 * Запись АРПМ в сечении
 */
public class ParameterArpm {

  private String name;
  private String uid;

  private List<String> UIDs = new ArrayList<>();

  public ParameterArpm(String name, String uid) {
    this.name = name;
    this.uid = uid;
    addID(uid);
  }

  public void addID(final String id) {
    if (id != null && id.length() == 36) {
      if (!UIDs.contains(id)) {
        UIDs.add(id);
      }
    }
  }

  public String getName() {
    return name;
  }

  public String getUid() {
    return uid;
  }

  @JsonIgnore
  public List<String> getUIDs() {
    return UIDs;
  }
}
