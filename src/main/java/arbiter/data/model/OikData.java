package arbiter.data.model;

public class OikData {
  //ОИК.адрес
  private String address;
  //ОИК Адрес авторизации
  private String authUrl;
  //ОИК.пользователь
  private String user;
  //ОИК.пароль
  private String password;
  //ОИК.отладка
  private boolean debug;

  public OikData() {
  }

  public OikData(String address, String authUrl, String user, String password, boolean debug) {
    this.address = address;
    this.authUrl = authUrl;
    this.user = user;
    this.password = password;
    this.debug = debug;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public String getAuthUrl() {
    return authUrl;
  }

  public void setAuthUrl(String authUrl) {
    this.authUrl = authUrl;
  }

  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public boolean isDebug() {
    return debug;
  }

  public void setDebug(boolean debug) {
    this.debug = debug;
  }
}
