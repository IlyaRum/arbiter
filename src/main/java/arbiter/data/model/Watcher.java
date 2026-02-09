package arbiter.data.model;

/**
 * Объект для сторожа
 */
public class Watcher {

  //id сторожа
  private String heartBeatUID;
  //Интервал сторожа
  private Integer heartBeatInterval;
  //Ждать сторожа
  private boolean watchDogWait;

  public Watcher() {
  }

  public Watcher(String heartBeatUID, Integer heartBeatInterval, boolean watchDogWait) {
    this.heartBeatUID = heartBeatUID;
    this.heartBeatInterval = heartBeatInterval;
    this.watchDogWait = watchDogWait;
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
}
