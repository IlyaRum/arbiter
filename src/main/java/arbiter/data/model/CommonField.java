package arbiter.data.model;

public class CommonField {
  //ОИК.адрес
  private String oikAddress;
  //ОИК.пользователь
  private String user;
  //ОИК.отладка
  private boolean debug;
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

  public String getOikAddress() {
    return oikAddress;
  }

  public void setOikAddress(String oikAddress) {
    this.oikAddress = oikAddress;
  }

  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public boolean isDebug() {
    return debug;
  }

  public void setDebug(boolean debug) {
    this.debug = debug;
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
