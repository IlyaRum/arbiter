package arbiter.data.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Обертка для формата запроса PUT
 */
public class UnitDataWrapper {
  @JsonProperty("sections")
  private List<UnitDto> section;

  public UnitDataWrapper(List<UnitDto> section) {
    this.section = section;
  }

  public List<UnitDto> getSection() {
    return section;
  }
}
