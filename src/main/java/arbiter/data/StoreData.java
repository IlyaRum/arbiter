package arbiter.data;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class StoreData {
  private final List<UnitDto> unitDtoList;

  public StoreData() {
    this.unitDtoList = new ArrayList<>();
  }

  public void addUnitData(UnitDto unitDto) {
    unitDtoList.add(unitDto);
  }

  public List<UnitDto> getUnitDataList() {
    return unitDtoList;
  }

  public int size() {
    return unitDtoList.stream()
      .mapToInt(unitDto -> unitDto.getParameters().size())
      .sum();
  }

  // Вспомогательный метод для поиска данных юнита
  public UnitDto getUnitData(Unit unit) {
    return unitDtoList.stream()
      .filter(ud -> ud.getUnit().equals(unit))
      .findFirst()
      .orElse(null);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", StoreData.class.getSimpleName() + "[", "]")
      .add("unitDataList=" + unitDtoList)
      .toString();
  }
}
