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

import static arbiter.constants.UnitCollectionConstants.*;
import static arbiter.util.ConfigValidator.*;

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
  private List<Integer> arpmStateTSList = new CopyOnWriteArrayList<>();
  private List<UnitResult> unitResults = new CopyOnWriteArrayList<>();


  @JsonIgnore
  private ErrorSet errorSet;
  @JsonIgnore
  private UltimateTimer cycleTimer;

  public Unit(int index, JsonObject config) {
    this.name = (String) validateFieldNameAndValueInSection(config.getString(CONFIG_KEY_UNIT_NAME), CONFIG_KEY_UNIT_NAME, CONFIG_KEY_UNITS_ARRAY);
    this.group = (String) validateFieldNameAndValueInSection(config.getString(CONFIG_KEY_UNIT_GROUP), CONFIG_KEY_UNIT_GROUP, CONFIG_KEY_UNITS_ARRAY);
    this.direction = validateFieldNameInSection(config, CONFIG_KEY_UNIT_DIRECTION, config.getInteger(CONFIG_KEY_UNIT_DIRECTION), CONFIG_KEY_UNITS_ARRAY);
    this.active = yesNo(config, CONFIG_KEY_UNIT_ACTIVE);
    this.mdpAndADP = yesNo(config, CONFIG_KEY_UNIT_CHECK_BOTH);
    this.deltaTm = validateFieldNameInSection(config, CONFIG_KEY_UNIT_DELTA_TM, config.getInteger(CONFIG_KEY_UNIT_DELTA_TM), CONFIG_KEY_UNITS_ARRAY);

    JsonArray influencingFactorArray = config.getJsonArray(CONFIG_KEY_INFLUENCING_FACTORS);
    if(influencingFactorArray !=null) {
      for (int i = 0; i < influencingFactorArray.size(); i++) {
        JsonObject influencingFactorObj = influencingFactorArray.getJsonObject(i);
        InfluencingFactor influencingFactor = new InfluencingFactor(
          validateFieldNameAndValueUuidInSection(CONFIG_KEY_INFLUENCING_FACTOR_ID,influencingFactorObj, CONFIG_KEY_INFLUENCING_FACTORS),
          (String) validateFieldNameAndValueInSection(influencingFactorObj.getString(CONFIG_KEY_INFLUENCING_FACTOR_NAME), CONFIG_KEY_INFLUENCING_FACTOR_NAME, CONFIG_KEY_INFLUENCING_FACTORS));
        influencingFactors.add(influencingFactor);
      }
    }

    JsonArray topologyArray = (JsonArray) validateFieldNameAndValueInSection(config.getJsonArray(CONFIG_KEY_TOPOLOGY), CONFIG_KEY_TOPOLOGY, CONFIG_KEY_UNITS_ARRAY);
      for (int i = 0; i < topologyArray.size(); i++) {
        JsonObject topologyObj = topologyArray.getJsonObject(i);
        Topology topology = new Topology(
          validateFieldNameAndValueUuidInSection(CONFIG_KEY_TOPOLOGY_ID, topologyObj, CONFIG_KEY_TOPOLOGY),
          (String) validateFieldNameAndValueInSection(topologyObj.getString(CONFIG_KEY_TOPOLOGY_NAME), CONFIG_KEY_TOPOLOGY_NAME, CONFIG_KEY_TOPOLOGY));
        topologies.add(topology);
    }

    JsonArray elementArray = (JsonArray) validateFieldNameAndValueInSection(config.getJsonArray(CONFIG_KEY_ELEMENTS), CONFIG_KEY_ELEMENTS, CONFIG_KEY_UNITS_ARRAY);
      for (int i = 0; i < elementArray.size(); i++) {
        JsonObject elementObj = elementArray.getJsonObject(i);
        Element element = new Element(
          validateFieldNameAndValueUuidInSection(CONFIG_KEY_ELEMENT_ID, elementObj, CONFIG_KEY_ELEMENTS),
          (String) validateFieldNameAndValueInSection(elementObj.getString(CONFIG_KEY_ELEMENT_NAME), CONFIG_KEY_ELEMENT_NAME, CONFIG_KEY_ELEMENTS));
        elements.add(element);
    }

    JsonObject repairSchemaObj = (JsonObject) validateFieldNameAndValueInSection(config.getJsonObject(CONFIG_KEY_REPAIR_SCHEMA), CONFIG_KEY_REPAIR_SCHEMA, CONFIG_KEY_UNITS_ARRAY);
      JsonArray TVSignals = repairSchemaObj.getJsonArray(CONFIG_KEY_REPAIR_SCHEMA_TV_SIGNALS);
      String checkFormula = repairSchemaObj.getString(CONFIG_KEY_REPAIR_SCHEMA_CHECK_FORMULA);
      repairSchema = new RepairSchema();
      repairSchema.setCheckFormula(checkFormula);
      for (int i = 0; i < TVSignals.size(); i++) {
        JsonObject signal = TVSignals.getJsonObject(i);
        RepairGroupValue repairGroupValue = new RepairGroupValue();
        repairGroupValue.setGroup(signal.getInteger(CONFIG_KEY_REPAIR_SCHEMA_GROUP));
        repairGroupValue.setOperation(signal.getString(CONFIG_KEY_REPAIR_SCHEMA_OPERATION));
        JsonArray composition = signal.getJsonArray(CONFIG_KEY_REPAIR_SCHEMA_COMPOSITION);
        for (int j = 0; j < composition.size(); j++) {
          JsonObject compositionObj = composition.getJsonObject(j);
          Composition compositionObject = new Composition(
            validateFieldNameAndValueUuidInSection(CONFIG_KEY_REPAIR_SCHEMA_COMPOSITION_ID, compositionObj, CONFIG_KEY_REPAIR_SCHEMA_COMPOSITION),
            (String) validateFieldNameAndValueInSection(compositionObj.getString(CONFIG_KEY_REPAIR_SCHEMA_COMPOSITION_NAME),CONFIG_KEY_REPAIR_SCHEMA_COMPOSITION_NAME,CONFIG_KEY_REPAIR_SCHEMA_COMPOSITION)
          );
          repairGroupValue.setComposition(compositionObject);
        }
        this.repairGroupValues.add(repairGroupValue);
      }
      repairSchema.setRepairGroupValues(repairGroupValues);

    JsonArray paramsArray = (JsonArray) validateFieldNameAndValueInSection(config.getJsonArray(CONFIG_KEY_PARAMETERS), CONFIG_KEY_PARAMETERS, CONFIG_KEY_UNITS_ARRAY);
    for (int i = 0; i < paramsArray.size(); i++) {
      JsonObject paramObj = paramsArray.getJsonObject(i);
      Parameter param = null;

      String parameterId = validateFieldNameAndValueUuidInSection(CONFIG_KEY_PARAMETER_ID, paramObj, CONFIG_KEY_PARAMETERS);
      String parameterName = (String) validateFieldNameAndValueInSection(paramObj.getString(CONFIG_KEY_PARAMETER_NAME), CONFIG_KEY_PARAMETER_NAME, CONFIG_KEY_PARAMETERS);
      if (paramObj.getInteger(CONFIG_KEY_PARAMETER_MIN) != null || paramObj.getInteger(CONFIG_KEY_PARAMETER_MAX) != null) {
        param = new Parameter(
          parameterName,
          parameterId,
          paramObj.getInteger(CONFIG_KEY_PARAMETER_MIN),
          paramObj.getInteger(CONFIG_KEY_PARAMETER_MAX));
      } else {
        param = new Parameter(
          parameterName,
          parameterId);
      }
      parameters.add(param);
    }

    JsonArray ARPMArray = config.getJsonArray(CONFIG_KEY_ARPM);
    if(ARPMArray !=null) {
      JsonArray arpmStateTSArray = (JsonArray) validateFieldNameAndValueInSection(config.getJsonArray(ARPM_PARAM_STATE_TS), ARPM_PARAM_STATE_TS, CONFIG_KEY_UNITS_ARRAY);
      arpmStateTSList.addAll(arpmStateTSArray.getList());
      for (int i = 0; i < ARPMArray.size(); i++) {
        JsonObject arpmObj = ARPMArray.getJsonObject(i);
        ARPM arpm = new ARPM();
        arpm.setName((String) validateFieldNameAndValueInSection(arpmObj.getString(CONFIG_KEY_ARPM_NAME), CONFIG_KEY_ARPM_NAME, CONFIG_KEY_ARPM));
        List<ParameterArpm> parameterArpms = new CopyOnWriteArrayList<>();
        parameterArpms.add(new ParameterArpm((String) validateFieldNameInSection(arpmObj.getString(ARPM_PARAM_ARBITR_NOT_VALID), ARPM_PARAM_ARBITR_NOT_VALID, CONFIG_KEY_ARPM),
          validateFieldNameAndValueUuidInSection(ARPM_PARAM_ARBITR_NOT_VALID, arpmObj, CONFIG_KEY_ARPM)));
        parameterArpms.add(new ParameterArpm((String) validateFieldNameInSection(arpmObj.getString(ARPM_PARAM_ADAPTIVE_SETPOINT_READ), ARPM_PARAM_ADAPTIVE_SETPOINT_READ, CONFIG_KEY_ARPM),
          validateFieldNameAndValueUuidInSection(ARPM_PARAM_ADAPTIVE_SETPOINT_READ, arpmObj, CONFIG_KEY_ARPM)));
        parameterArpms.add(new ParameterArpm((String) validateFieldNameInSection(arpmObj.getString(ARPM_PARAM_ADAPTIVE_SETPOINT_WRITE), ARPM_PARAM_ADAPTIVE_SETPOINT_WRITE, CONFIG_KEY_ARPM),
          validateFieldNameAndValueUuidInSection(ARPM_PARAM_ADAPTIVE_SETPOINT_WRITE, arpmObj, CONFIG_KEY_ARPM)));
        parameterArpms.add(new ParameterArpm((String) validateFieldNameInSection(arpmObj.getString(ARPM_PARAM_DELTA), ARPM_PARAM_DELTA, CONFIG_KEY_ARPM),
          validateFieldNameAndValueUuidInSection(ARPM_PARAM_DELTA, arpmObj, CONFIG_KEY_ARPM)));
        parameterArpms.add(new ParameterArpm((String) validateFieldNameInSection(arpmObj.getString(ARPM_PARAM_TZ), ARPM_PARAM_TZ, CONFIG_KEY_ARPM),
          validateFieldNameAndValueUuidInSection(ARPM_PARAM_TZ, arpmObj, CONFIG_KEY_ARPM)));
        parameterArpms.add(new ParameterArpm((String) validateFieldNameInSection(arpmObj.getString(ARPM_PARAM_EXCEED_WRITTEN), ARPM_PARAM_EXCEED_WRITTEN, CONFIG_KEY_ARPM),
          validateFieldNameAndValueUuidInSection(ARPM_PARAM_EXCEED_WRITTEN, arpmObj, CONFIG_KEY_ARPM)));
        parameterArpms.add(new ParameterArpm((String) validateFieldNameInSection(arpmObj.getString(ARPM_PARAM_EXCEED_PREVIOUS), ARPM_PARAM_EXCEED_PREVIOUS, CONFIG_KEY_ARPM),
          validateFieldNameAndValueUuidInSection(ARPM_PARAM_EXCEED_PREVIOUS, arpmObj, CONFIG_KEY_ARPM)));
        parameterArpms.add(new ParameterArpm((String) validateFieldNameInSection(arpmObj.getString(ARPM_PARAM_STATE), ARPM_PARAM_STATE, CONFIG_KEY_ARPM),
          validateFieldNameAndValueUuidInSection(ARPM_PARAM_STATE, arpmObj, CONFIG_KEY_ARPM)));
        arpm.setParameterArpm(parameterArpms);
        arpmList.add(arpm);
      }
    }

    JsonArray resultArray = (JsonArray) validateFieldNameAndValueInSection(config.getJsonArray(CONFIG_KEY_RESULTS), CONFIG_KEY_RESULTS, CONFIG_KEY_UNITS_ARRAY);
    if(resultArray !=null) {
      for (int i = 0; i < resultArray.size(); i++) {
        JsonObject resultObj = resultArray.getJsonObject(i);
        UnitResult result = new UnitResult(
            (String) validateFieldNameAndValueInSection(resultObj.getString(CONFIG_KEY_RESULT_NAME), CONFIG_KEY_RESULT_NAME, CONFIG_KEY_RESULTS),
            validateFieldNameAndValueUuidInSection(CONFIG_KEY_RESULT_ID, resultObj, CONFIG_KEY_RESULTS));
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

  public List<Integer> getArpmStateTSList() {
    return arpmStateTSList;
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
      .add("active=" + active)
      .add("deltaTm=" + deltaTm)
      .add("mdpAndADP=" + mdpAndADP)
      .toString();
  }
}
