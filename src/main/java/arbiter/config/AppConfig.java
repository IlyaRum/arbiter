package arbiter.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class AppConfig {
  public static final int HTTP_PORT = 8080;
  public static final String CORE_VERSION = "2.1";
  public static final String MEASUREMENT_VERSION = "2.1";

  public static final String INFO = "/info";
  public static final String METRICS = "/metrics";
  public static final String RECONNECTION_STATS = "/reconnection-stats";

  public static final String CORE_PREFIX = "/api/public/core/v" + CORE_VERSION;
  public static final String MEASUREMENT_PREFIX = "/api/public/measurement-values/v" + MEASUREMENT_VERSION;

  public static final String CHANNELS_OPEN = "/channels/open";
  public static final String FORCE_RECONNECT = "/force-reconnect";
  public static final String STOP_RECONNECTING = "/stop-reconnecting";
  public static final String ADD_SUBSCRIPTION_BY_CHANNELID = "/channels/:channelId/add-subscription";
  public static final String DELETE_SUBSCRIPTION = "/channels/:channelId/delete-subscription/:subscriptionId";
  public static final String CHANGE_SUBSCRIPTION = "/channels/:channelId/change-subscription/:subscriptionId";
  public static final String CLOSE_WEBSOCKET_CONNECT = "/close-websocket-connect";

  public static final String CLOUDEVENTS_PROTOCOL = "cloudevents.json";
  public static final String ARBITER_CONFIG_FILE = "arbiter_server.json";

  private static String authBasicCredentials;
  private static String authTokenUrl;
  private static String subscriptionsAddUrl;
  private static String subscriptionsChangeUrl;
  private static String subscriptionsDeleteUrl;
  private static String devFlag;

  public static void loadConfig() {
    // Чтение конфигурации из файла или системных свойств
    String configFile = System.getProperty("config.file", "config.properties");

    try {
      Properties props = new Properties();
      String filePath = ".\\" + configFile;
      File file = new File(filePath);
      if (!file.exists()) {
        filePath = ".\\src\\main\\resources\\" + configFile;
      }

      System.out.println("Config file is here: " + filePath);

      props.load(new FileInputStream(filePath));

      authBasicCredentials = props.getProperty("auth.basic.credentials");
      authTokenUrl = props.getProperty("auth.token.url");
      subscriptionsAddUrl = props.getProperty("subscriptions.add.url");
      subscriptionsChangeUrl = props.getProperty("subscriptions.change.url");
      subscriptionsDeleteUrl = props.getProperty("subscriptions.delete.url");
      devFlag =props.getProperty("dev.flag");

      if (authBasicCredentials == null ||
        authTokenUrl == null ||
        subscriptionsAddUrl == null ||
        subscriptionsChangeUrl == null ||
        subscriptionsDeleteUrl == null) {
        throw new RuntimeException("Missing required properties in config file");
      }

    } catch (IOException e) {
      throw new RuntimeException("Failed to load configuration file: " + configFile, e);
    }
  }

  public static String getAuthTokenUrl() {
    return authTokenUrl;
  }

  public static String getAuthBasicCredentials() {
    return authBasicCredentials;
  }

  public static String getSubscriptionsAddUrl() {
    return subscriptionsAddUrl;
  }

  public static String getSubscriptionsChangeUrl() {
    return subscriptionsChangeUrl;
  }

  public static String getSubscriptionsDeleteUrl() {
    return subscriptionsDeleteUrl;
  }

  public static String getDevFlag() {
    return devFlag;
  }
}
