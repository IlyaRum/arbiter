package arbiter.data.model;

public class CommonField {
  // Данные ОИК
  private OikData oikData;
  // Данные сторож
  private Watcher watcher;
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

  public CommonField() {
    this.oikData = new OikData();
    this.watcher = new Watcher();
  }

  public OikData getOikData() {
    return oikData;
  }

  public void setOikData(OikData oikData) {
    this.oikData = oikData;
  }

  public Watcher getWatcher() {
    return watcher;
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
    return this.watcher.getHeartBeatUID();
  }

  public void setHeartBeatUID(String heartBeatUID) {
    this.watcher.setHeartBeatUID(heartBeatUID);
  }

  public Integer getHeartBeatInterval() {
    return this.watcher.getHeartBeatInterval();
  }

  public void setHeartBeatInterval(Integer heartBeatInterval) {
    this.watcher.setHeartBeatInterval(heartBeatInterval);
  }

  public boolean isWatchDogWait() {
    return this.watcher.isWatchDogWait();
  }

  public void setWatchDogWait(boolean watchDogWait) {
    this.watcher.setWatchDogWait(watchDogWait);
  }

  public boolean isMinusHK() {
    return minusHK;
  }

  public void setMinusHK(boolean minusHK) {
    this.minusHK = minusHK;
  }
}
