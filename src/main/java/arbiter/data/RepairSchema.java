package arbiter.data;


import java.util.List;

/**
 * Ремонтная схема
 */

public class RepairSchema {
  private String checkFormula;
  private List<RepairGroupValue> repairGroupValues;

  public String getCheckFormula() {
    return checkFormula;
  }

  public void setCheckFormula(String checkFormula) {
    this.checkFormula = checkFormula;
  }

  public List<RepairGroupValue> getRepairGroupValues() {
    return repairGroupValues;
  }

  public void setRepairGroupValues(List<RepairGroupValue> repairGroupValues) {
    this.repairGroupValues = repairGroupValues;
  }
}
