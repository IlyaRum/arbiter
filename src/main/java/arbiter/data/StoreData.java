package arbiter.data;

import arbiter.data.dto.CommonFieldDto;
import arbiter.data.dto.UnitDto;
import arbiter.data.model.CommonField;
import arbiter.data.model.RepairGroupValue;
import arbiter.data.model.Unit;
import com.fasterxml.jackson.annotation.*;

import java.util.*;

@JsonIgnoreProperties({"unitDataList"})
public class StoreData {
  @JsonProperty("common")
  private CommonFieldDto commonFieldDto;
  @JsonProperty("section")
  private final List<UnitDto> unitDtoList;

  public StoreData() {
    this.commonFieldDto = new CommonFieldDto();
    this.unitDtoList = new ArrayList<>();
  }

  public void addUnitData(UnitDto unitDto) {
    unitDtoList.add(unitDto);
  }

  public List<UnitDto> getUnitDataList() {
    return unitDtoList;
  }

  public void setCommonFieldDto(CommonFieldDto commonFieldDto) {
    this.commonFieldDto = commonFieldDto;
  }

  public CommonFieldDto getCommonFieldDto() {
    return commonFieldDto;
  }

  public int size() {
    return unitDtoList.stream()
      .mapToInt(unitDto -> {
        int sizeCompositionIds = 0;

        if (unitDto.getRepairSchema() != null &&
          unitDto.getRepairSchema().getRepairGroupValues() != null) {
          sizeCompositionIds = unitDto.getRepairSchema()
            .getRepairGroupValues().stream()
            .map(RepairGroupValue::getValues)
            .filter(Objects::nonNull) // дополнительная проверка
            .flatMap(Collection::stream)
            .filter(Objects::nonNull) // проверка элементов
            .toList()
            .size();
        }

        return unitDto.getParameters().size() +
          unitDto.getTopologyList().size() +
          unitDto.getElements().size() +
          unitDto.getInfluencingFactors().size() +
          sizeCompositionIds +
          unitDto.getAutomaticPowerControls().size() +
          unitDto.getResult().size();
      })
      .sum();
  }

  public UnitDto getUnitData(Unit unit) {
    return unitDtoList.stream()
      .filter(ud -> ud.getUnit().equals(unit))
      .findFirst()
      .orElse(null);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", StoreData.class.getSimpleName() + "[", "]")
      .add("common=" + commonFieldDto)
      .add("section=" + unitDtoList)
      .toString();
  }
}
