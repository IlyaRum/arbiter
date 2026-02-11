package arbiter.data.model;

import arbiter.data.ErrorSet;
import arbiter.data.UltimateTimer;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Collection;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Сечение
 */

public class Unit {
  private String name;
  private String group;
  private int direction;
  private int deltaTm;
  private boolean active;
  private boolean mdpAndADP;
  private RepairSchema repairSchema;

  private List<Parameter> parameters = new CopyOnWriteArrayList<>();
  private List<Topology> topologies = new CopyOnWriteArrayList<>();
  private List<Element> elements = new CopyOnWriteArrayList<>();
  private List<InfluencingFactor> influencingFactors = new CopyOnWriteArrayList<>();
  private List<RepairGroupValue> repairGroupValues = new CopyOnWriteArrayList<>();
  private List<ARPM> arpmList = new CopyOnWriteArrayList<>();
  private List<UnitResult> unitResults = new CopyOnWriteArrayList<>();


  @JsonIgnore
  private ErrorSet errorSet;
  @JsonIgnore
  private UltimateTimer cycleTimer;

  public Unit(int index, JsonObject config) {
    this.name = config.getString("наименование");
    this.group = config.getString("группа");
    this.direction = config.getInteger("направление");
    this.active = yesNo(config, "в работе");
    this.mdpAndADP = yesNo(config, "проверять и МДП и АДП");
    this.deltaTm = config.getInteger("Дельта ТИ");

    JsonArray influencingFactorArray = config.getJsonArray("Влияющие ТИ");
    if(influencingFactorArray !=null) {
      for (int i = 0; i < influencingFactorArray.size(); i++) {
        JsonObject influencingFactorObj = influencingFactorArray.getJsonObject(i);
        InfluencingFactor influencingFactor = new InfluencingFactor(
          influencingFactorObj.getString("id"),
          influencingFactorObj.getString("имя"));
        influencingFactors.add(influencingFactor);
      }
    }

    JsonArray topologyArray = config.getJsonArray("топология");
    if(topologyArray !=null) {
      for (int i = 0; i < topologyArray.size(); i++) {
        JsonObject topologyObj = topologyArray.getJsonObject(i);
        Topology topology = new Topology(
          topologyObj.getString("id"),
          topologyObj.getString("имя"));
        topologies.add(topology);
      }
    }

    JsonArray elementArray = config.getJsonArray("ТС элементов");
    if(elementArray !=null) {
      for (int i = 0; i < elementArray.size(); i++) {
        JsonObject elementObj = elementArray.getJsonObject(i);
        Element element = new Element(
          elementObj.getString("id"),
          elementObj.getString("имя"));
        elements.add(element);
      }
    }

    JsonObject repairSchemaObj = config.getJsonObject("ремонтная схема");
    if(repairSchemaObj !=null) {
      JsonArray TVSignals = repairSchemaObj.getJsonArray("телесигналы");
      String checkFormula = repairSchemaObj.getString("проверка");
      repairSchema = new RepairSchema();
      repairSchema.setCheckFormula(checkFormula);
      for (int i = 0; i < TVSignals.size(); i++) {
        JsonObject signal = TVSignals.getJsonObject(i);
        RepairGroupValue repairGroupValue = new RepairGroupValue();
        repairGroupValue.setGroup(signal.getInteger("группа"));
        repairGroupValue.setOperation(signal.getString("операция"));
        JsonArray composition = signal.getJsonArray("состав");
        for (int j = 0; j < composition.size(); j++) {
          JsonObject compositionObj = composition.getJsonObject(j);
          String id = compositionObj.getString("id");
          Composition compositionObject = new Composition(
            id,
            compositionObj.getString("имя")
          );
          repairGroupValue.setComposition(compositionObject);
        }
        this.repairGroupValues.add(repairGroupValue);
      }
      repairSchema.setRepairGroupValues(repairGroupValues);
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

    JsonArray ARPMArray = config.getJsonArray("АРПМ");
    if(ARPMArray !=null) {
      for (int i = 0; i < ARPMArray.size(); i++) {
        JsonObject arpmObj = ARPMArray.getJsonObject(i);
        ARPM arpm = new ARPM();
        arpm.setName(arpmObj.getString("имя"));
        List<ParameterArpm> parameterArpms = new CopyOnWriteArrayList<>();
        parameterArpms.add(new ParameterArpm("Арбитр. Не пройдена достоверизация уставки", arpmObj.getString("Арбитр. Не пройдена достоверизация уставки")));
        parameterArpms.add(new ParameterArpm("АРПМ адаптивная уставка чтение", arpmObj.getString("АРПМ адаптивная уставка чтение")));
        parameterArpms.add(new ParameterArpm("АРПМ адаптивная уставка запись", arpmObj.getString("АРПМ адаптивная уставка запись")));
        parameterArpms.add(new ParameterArpm("АРПМ дельта", arpmObj.getString("АРПМ дельта")));
        parameterArpms.add(new ParameterArpm("АРПМ Тз", arpmObj.getString("АРПМ Тз")));
        parameterArpms.add(new ParameterArpm("АРПМ превышение записанного", arpmObj.getString("АРПМ превышение записанного")));
        parameterArpms.add(new ParameterArpm("АРПМ превышение предыдущего", arpmObj.getString("АРПМ превышение предыдущего")));
        parameterArpms.add(new ParameterArpm("состояние АРПМ", arpmObj.getString("состояние АРПМ")));
        arpm.setParameterArpm(parameterArpms);
        arpmList.add(arpm);
      }
    }

    JsonArray resultArray = config.getJsonArray("результат");
    if(resultArray !=null) {
      for (int i = 0; i < resultArray.size(); i++) {
        JsonObject resultObj = resultArray.getJsonObject(i);
        UnitResult result = new UnitResult(
            resultObj.getString("имя"),
            resultObj.getString("id"));
        unitResults.add(result);
      }
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

  public List<Topology> getTopologies() {
    return topologies;
  }

  public List<Element> getElements() {
    return elements;
  }

  public List<InfluencingFactor> getInfluencingFactors() {
    return influencingFactors;
  }

  public RepairSchema getRepairSchema() {
    return repairSchema;
  }

  @JsonIgnore
  public Iterable<Parameter> getAllParameters() {
    return parameters;
  }

  public String getName() {
    return name;
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

  public boolean isMdpAndADP() {
    return mdpAndADP;
  }

  public int getDeltaTm() {
    return deltaTm;
  }

  public List<ARPM> getArpmList() {
    return arpmList;
  }

  public List<UnitResult> getUnitResults() {
    return unitResults;
  }

  public List<ParameterArpm> getParameterArpmList() {
   return arpmList.stream()
     .map(ARPM::getParameterArpm)
     .flatMap(Collection::stream)
     .toList();
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", Unit.class.getSimpleName() + "[", "]")
      .add("name='" + name + "'")
      .add("group='" + group + "'")
      .add("direction=" + direction)
      .add("writeResultToScada=" + active)
      .add("deltaTm=" + deltaTm)
      .add("mdpAndADP=" + mdpAndADP)
      .toString();
  }
}
