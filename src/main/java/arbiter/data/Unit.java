package arbiter.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Сечение
 */

public class Unit {
  //private int index;
  private String name;
  private String group;
  private int direction;
  private int deltaTm;
  private boolean writeResultToScada;
  private boolean mdpAndADP;
  //private String eventObject;

  private Topology topology;
  private Element element;
  private InfluencingFactor influencingFactor;

  //@JsonIgnore
  private List<Parameter> parameters = new CopyOnWriteArrayList<>();

  private Map<String, String> topologies = new ConcurrentHashMap<>();
  private Map<String, String> elements = new ConcurrentHashMap<>();
  private Map<String, String> influencingFactors = new ConcurrentHashMap<>();

//  @JsonIgnore
//  private Map<String, Result> results = new ConcurrentHashMap<>();
  @JsonIgnore
  private ErrorSet errorSet;
  @JsonIgnore
  private UltimateTimer cycleTimer;

  public Unit(int index, JsonObject config) {
//    this.index = index;
    this.name = config.getString("наименование");
    this.group = config.getString("группа");
    this.direction = config.getInteger("направление");
    this.writeResultToScada = yesNo(config, "в работе");
    this.mdpAndADP = yesNo(config, "проверять и МДП и АДП");
    this.deltaTm = config.getInteger("Дельта ТИ");

    JsonArray influencingFactorArray = config.getJsonArray("Влияющие ТИ");
    for (int i = 0; i < influencingFactorArray.size(); i++) {
      JsonObject influencingFactorObj = influencingFactorArray.getJsonObject(i);
      InfluencingFactor influencingFactor = new InfluencingFactor(
        influencingFactorObj.getString("id"),
        influencingFactorObj.getString("имя"));
      influencingFactors.put(influencingFactorObj.getString("имя"), influencingFactorObj.getString("id"));
    }

    JsonArray topologyArray = config.getJsonArray("топология");
    for (int i = 0; i < topologyArray.size(); i++) {
      JsonObject topologyObj = topologyArray.getJsonObject(i);
      Topology topology = new Topology(
        topologyObj.getString("id"),
        topologyObj.getString("имя"));
      topologies.put(topologyObj.getString("имя"), topologyObj.getString("id"));
    }

    JsonArray elementArray = config.getJsonArray("ТС элементов");
    for (int i = 0; i < elementArray.size(); i++) {
      JsonObject elementObj = elementArray.getJsonObject(i);
      Element element = new Element(
        elementObj.getString("id"),
        elementObj.getString("имя"));
      elements.put(elementObj.getString("имя"), elementObj.getString("id"));
    }

    // Инициализация параметров и результатов
    JsonArray paramsArray = config.getJsonArray("исходные данные");
    for (int i = 0; i < paramsArray.size(); i++) {
      JsonObject paramObj = paramsArray.getJsonObject(i);
      Parameter param = null;

      if (paramObj.getInteger("min") != null || paramObj.getInteger("max") != null) {
        param = new Parameter(
          paramObj.getString("имя"),
          paramObj.getString("id"),
          paramObj.getInteger("min"),
          paramObj.getInteger("max"));
      } else {
        param = new Parameter(
          paramObj.getString("имя"),
          paramObj.getString("id"));
      }
      parameters.add(param);
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
    return parameters.stream()
      .filter(param -> name.equals(param.getName()))
      .findFirst()
      .orElse(null);
  }

  public void addParameter(Parameter parameter) {
    parameters.removeIf(param -> param.getName().equals(parameter.getName()));
    parameters.add(parameter);
  }

  public boolean containsParameter(String name) {
    return parameters.stream()
      .anyMatch(param -> name.equals(param.getName()));
  }

//  public Result getResult(String name) {
//    return results.get(name);
//  }

  public boolean checkCircuit() {
    // Реализация проверки схемы
    return false;
  }

  public boolean checkTimer() {
    return cycleTimer.check();
  }

  public List<Parameter> getParameters() {
    return parameters;
  }

  // Аналог Items[j].Parameters.Data[k]
  @JsonIgnore
  public Iterable<Parameter> getAllParameters() {
    return parameters;
  }

  public String getName() {
    return name;
  }

//  public String getEventObject() {
//    return eventObject;
//  }

  public boolean isWriteResultToScada() {
    return writeResultToScada;
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

//  public int getIndex() {
//    return index;
//  }

  public boolean isMdpAndADP() {
    return mdpAndADP;
  }

//  public Map<String, Result> getResults() {
//    return results;
//  }

  public int getDeltaTm() {
    return deltaTm;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", Unit.class.getSimpleName() + "[", "]")
//      .add("index=" + index)
      .add("name='" + name + "'")
      .add("group='" + group + "'")
      .add("direction=" + direction)
      .add("writeResultToScada=" + writeResultToScada)
      .add("deltaTm=" + deltaTm)
      .add("mdpAndADP=" + mdpAndADP)
//      .add("parameters=" + parameters)
//      .add("results=" + results)
//      .add("eventObject='" + eventObject + "'")
//      .add("errorSet=" + errorSet)
//      .add("cycleTimer=" + cycleTimer)
      .toString();
  }
}
