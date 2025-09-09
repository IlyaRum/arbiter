package arbiter.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class AppConfig {
  public static final int HTTP_PORT = 8080;
  public static final String CORE_VERSION = "2.1";
  public static final String MEASUREMENT_VERSION = "2.1";
  public static final String CORE_PREFIX = "/api/public/core/v" + CORE_VERSION;
  public static final String MEASUREMENT_PREFIX = "/api/public/measurement-values/v" + MEASUREMENT_VERSION;
  public static final String CHANNELS_OPEN = "/channels/open";
  public static final String ADD_SUBSCRIPTION_BY_CHANNELID = "/channels/:channelId/add-subscription";
  public static final String DELETE_SUBSCRIPTION = "/channels/:channelId/delete-subscription/:subscriptionId";
  public static final String CHANGE_SUBSCRIPTION = "/channels/:channelId/change-subscription/:subscriptionId";
  public static final String CLOUDEVENTS_PROTOCOL = "cloudevents.json";

  private static String authBasicCredentials;
  private static String authTokenUrl;
  private static String subscriptionsAddUrl;
  private static String subscriptionsChangeUrl;
  private static String subscriptionsDeleteUrl;

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
}
