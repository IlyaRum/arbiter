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
  //экземпляр
  private String instance;
  //не проверять данные при старте 2 цикла
  private boolean skipCycle;
  //вычитать НК
  private boolean minusHK;
  //секунд между циклом расчета и критерием
  private Integer eventDelta;
  //порт арбитра
  private Integer port;
  //интервал опроса данных
  private Integer requestDelay;
  //количество попыток соединения с ОИК
  private Integer connectAttempt;
  //пауза перед соединением с ОИК
  private Integer oikConnectTimeout;

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

  public String getAuthUrl() {
    return oikData.getAuthUrl();
  }

  public void setAuthUrl(String authUrl) {
    oikData.setAuthUrl(authUrl);
  }

  public String getMeasurementUrl() {
    return oikData.getMeasurementUrl();
  }

  public void setMeasurementUrl(String measurementUrl) {
    oikData.setMeasurementUrl(measurementUrl);
  }

  public String getRuntimeUrl() {
    return oikData.getRuntimeUrl();
  }

  public void setRuntimeUrl(String runtimeUrl) {
    oikData.setRuntimeUrl(runtimeUrl);
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

  public String getInstance() {
    return instance;
  }

  public void setInstance(String instance) {
    this.instance = instance;
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

  public void setRequestDelay(Integer requestDelay) {
    this.requestDelay = requestDelay;
  }

  public Integer getConnectAttempt() {
    return connectAttempt;
  }

  public void setConnectAttempt(Integer connectAttempt) {
    this.connectAttempt = connectAttempt;
  }

  public Integer getOikConnectTimeout() {
    return oikConnectTimeout;
  }

  public void setOikConnectTimeout(Integer oikConnectTimeout) {
    this.oikConnectTimeout = oikConnectTimeout;
  }
}
