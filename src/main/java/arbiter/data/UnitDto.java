package arbiter.data;

//import arbiter.data.serialize.ParametersMapSerializer;
import arbiter.constants.ParameterMappingConstants;
import com.fasterxml.jackson.annotation.JsonIgnore;
//import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
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

  public UnitDto(Unit unit) {
    this.unit = unit;
    this.name = unit.getName();
    this.group = unit.getGroup();
    this.direction = unit.getDirection();
    this.deltaTm = unit.getDeltaTm();
    this.writeResultToScada = unit.isWriteResultToScada();
    this.mdpAndADP = unit.isMdpAndADP();
    this.parameters = unit.getParameters().stream().collect(Collectors.toMap(this::getMappedParameterKey, Function.identity(),(existing, replacement) -> existing,HashMap::new));
    this.topologyList = unit.getTopologies();
    this.elements = unit.getElements();
    this.influencingFactors = unit.getInfluencingFactors();
    this.repairSchema = unit.getRepairSchema();
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

  public RepairSchema getRepairschema() {
    return repairSchema;
  }

  private String getMappedParameterKey(Parameter parameter) {
    return ParameterMappingConstants.PARAMETER_NAME_TO_FIELD_MAPPING.getOrDefault(parameter.getName(), parameter.getId());
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", UnitDto.class.getSimpleName() + "[", "]")
      .add("unit=" + unit)
      .add("parameters=" + getParameters())
      .add("topologyList=" + getTopologyList())
      .add("elements=" + getElements())
      .add("repairSchema=" + getRepairschema())
      .add("influencingFactors=" + getInfluencingFactors())
      .toString();
  }
}
