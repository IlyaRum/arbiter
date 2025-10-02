package arbiter.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;

public class Unit {
  private int index;
  private String name;
  private String group;
  private int direction;
  private boolean active;
  private boolean mdpAndADP;
  private String eventObject;

  @JsonIgnore
  private Map<String, Parameter> parameters = new ConcurrentHashMap<>();
  private Map<String, Result> results = new ConcurrentHashMap<>();

  private ErrorSet errorSet;
  private UltimateTimer cycleTimer;
  // Другие таймеры...

  public Unit(int index, JsonObject config) {
    this.index = index;
    this.name = config.getString("наименование");
    this.group = config.getString("группа");
    this.direction = config.getInteger("направление");
    this.active = yesNo(config, "в работе");
    this.mdpAndADP = yesNo(config, "проверять и МДП и АДП");

    // Инициализация параметров и результатов
    JsonArray paramsArray = config.getJsonArray("исходные данные");
    for (int i = 0; i < paramsArray.size(); i++) {
      JsonObject paramObj = paramsArray.getJsonObject(i);
      Parameter param = new Parameter(
        paramObj.getString("имя"),
        paramObj.getString("id"));
      parameters.put(param.getName(), param);
    }

    // Инициализация таймеров
    this.cycleTimer = new UltimateTimer(this.name, "(ЦИКЛ)");
    this.errorSet = new ErrorSet(name);
  }

  public boolean yesNo(JsonObject obj, String key) {
    if (!obj.containsKey(key)) return false;
    return "да".equalsIgnoreCase(obj.getString(key));
  }

  public Parameter getParameter(String name) {
    return parameters.get(name);
  }

  public Result getResult(String name) {
    return results.get(name);
  }

  public boolean checkCircuit() {
    // Реализация проверки схемы
    return false;
  }

  public boolean checkTimer() {
    return cycleTimer.check();
  }

  public Map<String, Parameter> getParameters() {
    return parameters;
  }

  // Аналог Items[j].Parameters.Data[k]
  public Iterable<Parameter> getAllParameters() {
    return parameters.values();
  }

  public String getName() {
    return name;
  }

  public String getEventObject() {
    return eventObject;
  }

  public boolean isActive() {
    return active;
  }

  public UltimateTimer getCycleTimer() {
    return cycleTimer;
  }

  public int getDirection() {
    return direction;
  }

  public ErrorSet getErrorSet() {
    return errorSet;
  }

  public String getGroup() {
    return group;
  }

  public int getIndex() {
    return index;
  }

  public boolean isMdpAndADP() {
    return mdpAndADP;
  }

  public Map<String, Result> getResults() {
    return results;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", Unit.class.getSimpleName() + "[", "]")
      .add("active=" + active)
      .add("index=" + index)
      .add("name='" + name + "'")
      .add("group='" + group + "'")
      .add("direction=" + direction)
      .add("mdpAndADP=" + mdpAndADP)
      .add("eventObject='" + eventObject + "'")
      .add("parameters=" + parameters)
      .add("results=" + results)
      .add("errorSet=" + errorSet)
      .add("cycleTimer=" + cycleTimer)
      .toString();
  }
}
