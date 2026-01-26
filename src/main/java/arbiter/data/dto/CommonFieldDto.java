package arbiter.data.dto;

import arbiter.data.model.CommonField;
import arbiter.data.model.OikData;

/**
 * Класс для передачи общих данных в расчетный сервис
 */
public class CommonFieldDto {

  private OikData oik;
  private boolean writeEnable;
  private String eventUID;
  private String writeEventUID;
  private boolean skipCycle;
  private boolean minusHK;

  private String heartBeatUID;
  private Integer heartBeatInterval;
  private boolean watchDogWait;

  public CommonFieldDto() {
  }

  public CommonFieldDto(CommonField commonField) {
    if (commonField != null) {
      this.oik = commonField.getOikData() != null
        ? new OikData(
        commonField.getOikAddress(),
        commonField.getUser(),
        commonField.getPassword(),
        commonField.isDebug()
      )
        : new OikData();
      this.writeEnable = commonField.isWriteEnable();
      this.eventUID = commonField.getEventUID();
      this.writeEventUID = commonField.getWriteEventUID();
      this.skipCycle = commonField.isSkipCycle();
      this.minusHK = commonField.isMinusHK();
      this.heartBeatUID = commonField.getHeartBeatUID();
      this.heartBeatInterval = commonField.getHeartBeatInterval();
      this.watchDogWait = commonField.isWatchDogWait();
    }
  }

  public OikData getOik() {
    return oik;
  }

  public void setOik(OikData oik) {
    this.oik = oik;
  }

  public boolean isWriteEnable() {
    return writeEnable;
  }

  public void setWriteEnable(boolean writeEnable) {
    this.writeEnable = writeEnable;
  }

  public String getEventUID() {
    return eventUID;
  }

  public void setEventUID(String eventUID) {
    this.eventUID = eventUID;
  }

  public String getWriteEventUID() {
    return writeEventUID;
  }

  public void setWriteEventUID(String writeEventUID) {
    this.writeEventUID = writeEventUID;
  }

  public boolean isSkipCycle() {
    return skipCycle;
  }

  public void setSkipCycle(boolean skipCycle) {
    this.skipCycle = skipCycle;
  }

  public String getHeartBeatUID() {
    return heartBeatUID;
  }

  public void setHeartBeatUID(String heartBeatUID) {
    this.heartBeatUID = heartBeatUID;
  }

  public Integer getHeartBeatInterval() {
    return heartBeatInterval;
  }

  public void setHeartBeatInterval(Integer heartBeatInterval) {
    this.heartBeatInterval = heartBeatInterval;
  }

  public boolean isWatchDogWait() {
    return watchDogWait;
  }

  public void setWatchDogWait(boolean watchDogWait) {
    this.watchDogWait = watchDogWait;
  }

  public boolean isMinusHK() {
    return minusHK;
  }

  public void setMinusHK(boolean minusHK) {
    this.minusHK = minusHK;
  }
}
