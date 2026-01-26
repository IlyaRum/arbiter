package arbiter.data.model;

public class CommonField {
  // Данные ОИК
  private OikData oikData;
  //запись в ОИК
  private boolean writeEnable;
  //изменение критерия МДП СМЗУ
  private String eventUID;
  //запись критерия МДП СМЗУ
  private String writeEventUID;
  //не проверять данные при старте 2 цикла
  private boolean skipCycle;
  //вычитать НК
  private boolean minusHK;
  //id сторожа
  private String heartBeatUID;
  //Интервал сторожа
  private Integer heartBeatInterval;
  //Ждать сторожа
  private boolean watchDogWait;

  public CommonField() {
    this.oikData = new OikData();
  }

  public OikData getOikData() {
    return oikData;
  }

  public void setOikData(OikData oikData) {
    this.oikData = oikData;
  }

  public String getOikAddress() {
    return oikData.getAddress();
  }

  public void setOikAddress(String oikAddress) {
    this.oikData.setAddress(oikAddress);
  }

  public String getUser() {
    return oikData.getUser();
  }

  public void setUser(String user) {
    oikData.setUser(user);
  }

  public String getPassword() {
    return oikData.getPassword();
  }

  public void setPassword(String password) {
    oikData.setPassword(password);
  }

  public boolean isDebug() {
    return oikData.isDebug();
  }

  public void setDebug(boolean debug) {
    oikData.setDebug(debug);
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
