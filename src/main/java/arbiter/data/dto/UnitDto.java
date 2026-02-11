package arbiter.data.dto;

//import arbiter.data.serialize.ParametersMapSerializer;
import arbiter.constants.ParameterArpmMappingConstants;
import arbiter.constants.ParameterMappingConstants;
import arbiter.constants.ParameterUnitResultMappingConstants;
import arbiter.data.model.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
//import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class UnitDto {
  @JsonIgnore
  private final Unit unit;
  private final String name;
  private final String group;
  private final int direction;
  private final int deltaTm;
  private final boolean writeResultToScada;
  private final boolean mdpAndADP;
  private final List<Topology> topologyList;
  private final List<Element> elements;
  private final List<InfluencingFactor> influencingFactors;
  private final RepairSchema repairSchema;
  //@JsonSerialize(using = ParametersMapSerializer.class)
  private final Map<String, Parameter> parameters;
  private final Map<String, UnitResult> result;

  private List<Map<String, Object>> automaticPowerControls;

  public UnitDto(Unit unit) {
    this.unit = unit;
    this.name = unit.getName();
    this.group = unit.getGroup();
    this.direction = unit.getDirection();
    this.deltaTm = unit.getDeltaTm();
    this.writeResultToScada = unit.isActive();
    this.mdpAndADP = unit.isMdpAndADP();
    this.parameters = unit.getParameters().stream().collect(Collectors.toMap(this::getMappedParameterKey, Function.identity(),(existing, replacement) -> existing,HashMap::new));
    this.topologyList = unit.getTopologies();
    this.elements = unit.getElements();
    this.influencingFactors = unit.getInfluencingFactors();
    this.repairSchema = unit.getRepairSchema();

    this.automaticPowerControls = unit.getArpmList().stream()
      .map(this::convertArpmToMap)
      .collect(Collectors.toList());

    this.result = unit.getUnitResults().stream().collect(Collectors.toMap(this::getMappedUnitResultKey, Function.identity(),(existing, replacement) -> existing,LinkedHashMap::new));;
  }

  private Map<String, Object> convertArpmToMap(ARPM arpm) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("name", arpm.getName());
    map.putAll(arpm.getParameterArpm().stream()
      .collect(Collectors.toMap(
        this::getMappedParameterArpmKey,
        Function.identity(),
        (existing, replacement) -> existing,
        LinkedHashMap::new
      )));
    return map;
  }

  public Unit getUnit() {
    return unit;
  }

  public Map<String, Parameter> getParameters() {
    return new HashMap<>(parameters);
  }

  public String getName() {
    return name;
  }

  public String getGroup() {
    return group;
  }

  public int getDirection() {
    return direction;
  }

  public int getDeltaTm() {
    return deltaTm;
  }

  public boolean isWriteResultToScada() {
    return writeResultToScada;
  }

  public boolean isMdpAndADP() {
    return mdpAndADP;
  }

  public List<Topology> getTopologyList() {
    return topologyList;
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

  public List<Map<String, Object>> getAutomaticPowerControls() {
    return automaticPowerControls;
  }

  public Map<String, UnitResult> getResult() {
    return result;
  }

  private String getMappedParameterKey(Parameter parameter) {
    return ParameterMappingConstants.PARAMETER_NAME_TO_FIELD_MAPPING.getOrDefault(parameter.getName(), parameter.getId());
  }

  private String getMappedParameterArpmKey(ParameterArpm parameterArpm) {
    return ParameterArpmMappingConstants.PARAMETER_ARPM_NAME_TO_FIELD_MAPPING.getOrDefault(parameterArpm.getName(), parameterArpm.getUid());
  }

  private String getMappedUnitResultKey(UnitResult unitResult) {
    return ParameterUnitResultMappingConstants.PARAMETER_RESULT_NAME_TO_FIELD_MAPPING.getOrDefault(unitResult.getName(), unitResult.getUid());
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", UnitDto.class.getSimpleName() + "[", "]")
      .add("unit=" + unit)
      .add("parameters=" + getParameters())
      .add("topologyList=" + getTopologyList())
      .add("elements=" + getElements())
      .add("repairSchema=" + getRepairSchema())
      .add("influencingFactors=" + getInfluencingFactors())
      .toString();
  }
}
