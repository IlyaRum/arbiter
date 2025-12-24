package arbiter.data.dto;

import arbiter.data.model.Element;
import arbiter.data.model.InfluencingFactor;
import arbiter.data.model.Parameter;
import arbiter.data.model.Topology;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Класс для UnitDto с фильтрованными параметрами и топологией
 */
public class FilteredUnitDto extends UnitDto {
  private final Map<String, Parameter> filteredParameters;
  private final Map<String, Topology> filteredTopologies;
  private final List<Topology> filteredTopologyList;
  private final List<Element> filteredElementList;
  private final Map<String, Element> filteredElements;
  private final List<InfluencingFactor> filteredInfuencingFactorList;
  private final Map<String, InfluencingFactor> influencingFactories;

  public FilteredUnitDto(UnitDto original,
                         Map<String, Parameter> filteredParameters,
                         Map<String, Topology> filteredTopologies,
                         Map<String, Element> filteredElements,
                         Map<String, InfluencingFactor> influencingFactories) {
    super(original.getUnit());
    this.filteredParameters = filteredParameters;
    this.filteredTopologies = filteredTopologies;
    this.filteredTopologyList = new ArrayList<>(filteredTopologies.values());
    this.filteredElements = filteredElements;
    this.influencingFactories = influencingFactories;
    this.filteredElementList = new ArrayList<>(filteredElements.values());
    this.filteredInfuencingFactorList = new ArrayList<>(influencingFactories.values());
  }

  @Override
  public Map<String, Parameter> getParameters() {
    return filteredParameters;
  }

  @Override
  public List<Topology> getTopologyList() {
    return filteredTopologyList;
  }

  @Override public List<Element> getElements() {
    return filteredElementList;
  }

  @Override
  public List<InfluencingFactor> getInfluencingFactors() {
    return filteredInfuencingFactorList;
  }
}
