package data;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Unit {
  private int index;
  private String name;
  private String group;
  private int direction;
  private boolean active;
  private boolean mdpAndADP;
  private String eventObject;

  private Map<String, Parameter> parameters = new ConcurrentHashMap<>();
  private Map<String, Result> results = new ConcurrentHashMap<>();

  private ErrorSet errorSet;
  private UltimateTimer cycleTimer;
  // Другие таймеры...

  private UnitCollection collection;

  public Unit(int index, JsonObject config, UnitCollection collection) {
    this.index = index;
    this.collection = collection;
    this.name = config.getString("наименование");
    this.group = config.getString("группа");
    this.direction = config.getInteger("направление");
    this.active = collection.yesNo(config, "в работе");
    this.mdpAndADP = collection.yesNo(config, "проверять и МДП и АДП");

    // Инициализация параметров и результатов
    JsonArray paramsArray = config.getJsonArray("исходные данные");
    for (int i = 0; i < paramsArray.size(); i++) {
      JsonObject paramObj = paramsArray.getJsonObject(i);
      Parameter param = new Parameter(this,
        paramObj.getString("имя"),
        paramObj.getString("id"));
      parameters.put(param.getName(), param);
    }

    // Инициализация таймеров
    this.cycleTimer = new UltimateTimer(this.name, "(ЦИКЛ)");
    this.errorSet = new ErrorSet(name);
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

  public UnitCollection getCollection() {
    return collection;
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
}
