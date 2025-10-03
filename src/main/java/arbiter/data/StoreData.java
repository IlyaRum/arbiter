package arbiter.data;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class StoreData {
  private final List<UnitData> unitDataList;

  public StoreData() {
    this.unitDataList = new ArrayList<>();
  }

  public void addUnitData(UnitData unitData) {
    unitDataList.add(unitData);
  }

  public List<UnitData> getUnitDataList() {
    return unitDataList;
  }

  public int size() {
    return unitDataList.stream()
      .mapToInt(unitData -> unitData.getParameters().size())
      .sum();
  }

  // Вспомогательный метод для поиска данных юнита
  public UnitData getUnitData(Unit unit) {
    return unitDataList.stream()
      .filter(ud -> ud.getUnit().equals(unit))
      .findFirst()
      .orElse(null);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", StoreData.class.getSimpleName() + "[", "]")
      .add("unitDataList=" + unitDataList)
      .toString();
  }
}
