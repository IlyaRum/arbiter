package arbiter.data.dto;

import arbiter.data.model.CommonField;
import arbiter.data.model.OikData;
import arbiter.data.model.Watcher;

/**
 * Класс для передачи общих данных в расчетный сервис
 */
public class CommonFieldDto {

  private OikData oik;
  private Watcher watcher;
  private boolean writeEnable;
  private String eventUID;
  private String writeEventUID;
  private boolean skipCycle;
  private boolean minusHK;
  private Integer eventDelta;
  private Integer port;
  private Integer requestDelay;
  private Integer connectAttempt;
  private Integer oikConnectTimeout;

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
      this.eventDelta = commonField.getEventDelta();
      this.port = commonField.getPort();
      this.requestDelay = commonField.getRequestDelay();
      this.connectAttempt = commonField.getConnectAttempt();
      this.oikConnectTimeout = commonField.getOikConnectTimeout();
      this.watcher = commonField.getWatcher() != null
        ? new Watcher(
        commonField.getHeartBeatUID(),
        commonField.getHeartBeatInterval(),
        commonField.isWatchDogWait()
      ) : new Watcher();
    }
  }

  public OikData getOik() {
    return oik;
  }

  public void setOik(OikData oik) {
    this.oik = oik;
  }

  public Watcher getWatcher() {
    return watcher;
  }

  public void setWatcher(Watcher watcher) {
    this.watcher = watcher;
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

  public boolean isMinusHK() {
    return minusHK;
  }

  public void setMinusHK(boolean minusHK) {
    this.minusHK = minusHK;
  }

  public Integer getEventDelta() {
    return eventDelta;
  }

  public void setEventDelta(Integer eventDelta) {
    this.eventDelta = eventDelta;
  }

  public Integer getPort() {
    return port;
  }

  public void setPort(Integer port) {
    this.port = port;
  }

  public Integer getRequestDelay() {
    return requestDelay;
  }

  public Integer getConnectAttempt() {
    return connectAttempt;
  }

  public Integer getOikConnectTimeout() {
    return oikConnectTimeout;
  }


}
