package arbiter.data.dto;

import arbiter.data.model.CommonField;

/**
 * Класс для передачи общих данных в расчетный сервис
 */
public class CommonFieldDto {

  private String oikAddress;

  public CommonFieldDto() {
  }

  public CommonFieldDto(CommonField commonField) {
    if (commonField != null) {
      this.oikAddress = commonField.getOik();
    }
  }

  public String getOikAddress() {
    return oikAddress;
  }

  public void setOikAddress(String oikAddress) {
    this.oikAddress = oikAddress;
  }
}
