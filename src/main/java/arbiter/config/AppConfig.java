package arbiter.config;

import arbiter.util.ConfigValidator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

public class AppConfig {
  public static final int HTTP_PORT = 8080;
  public static final String CORE_VERSION = "2.1";
  public static final String MEASUREMENT_VERSION = "2.1";

  public static final String INFO = "/info";
  public static final String METRICS = "/metrics";
  public static final String RECONNECTION_STATS = "/reconnection-stats";
  public static final String HEALTH = "/health";

  public static final String CORE_PREFIX = "/api/public/core/v" + CORE_VERSION;
  public static final String MEASUREMENT_PREFIX = "/api/public/measurement-values/v" + MEASUREMENT_VERSION;

  public static final String CHANNELS_OPEN = "/channels/open";
  public static final String FORCE_RECONNECT = "/force-reconnect";
  public static final String STOP_RECONNECTING = "/stop-reconnecting";
  public static final String ADD_SUBSCRIPTION_BY_CHANNELID = "/channels/:channelId/add-subscription";
  public static final String DELETE_SUBSCRIPTION = "/channels/:channelId/delete-subscription/:subscriptionId";
  public static final String CHANGE_SUBSCRIPTION = "/channels/:channelId/change-subscription/:subscriptionId";
  public static final String ADD_EVENT_SUBSCRIPTION = "/channels/:channelId/event-types/add";
  public static final String CLOSE_WEBSOCKET_CONNECT = "/close-websocket-connect";

  public static final String CLOUDEVENTS_PROTOCOL = "cloudevents.json";

  private static String subscriptionsAddUrl;
  private static String subscriptionsChangeUrl;
  private static String subscriptionsDeleteUrl;
  private static String eventSubscriptionsAddUrl;
  private static String devFlag;
  private static String arbiterConfigJsonFile;
  private static String calcSrvUrl;
  private static String oikCertCrt;
  private static Boolean isTrust;
  private static String pingInterval;
  private static Boolean enablePing;

  public static void loadConfig() {
    String configFile = System.getProperty("config.file", "configData.json");

    try {
      Properties props = new Properties();
      String filePath = "./" + configFile;
      File file = new File(filePath);
      if (!file.exists()) {
        filePath = "./src/main/resources/" + configFile;
        file = new File(filePath);
      }

      System.out.println("Config file is here: " + filePath);

      ObjectMapper mapper = new ObjectMapper();
      props.putAll(mapper.readValue(file, new TypeReference<>() {}));

      subscriptionsAddUrl = ConfigValidator.checkValueProperty(props, "subscriptions.add.url", configFile);
      subscriptionsChangeUrl = ConfigValidator.checkValueProperty(props, "subscriptions.change.url", configFile);
      subscriptionsDeleteUrl = ConfigValidator.checkValueProperty(props, "subscriptions.delete.url", configFile);
      eventSubscriptionsAddUrl = props.getProperty("event.subscriptions.add");
      devFlag = props.getProperty("dev.flag");
      arbiterConfigJsonFile = ConfigValidator.checkValueProperty(props,"arbiter.config.json", configFile);
      calcSrvUrl = ConfigValidator.checkValueProperty(props, "calc-srv.absolute.url", configFile);
      oikCertCrt = ConfigValidator.checkValueProperty(props, "oik.cert.crt", configFile);
      isTrust = Boolean.parseBoolean(props.getProperty("trust.all", "false"));
      pingInterval =  ConfigValidator.checkValueProperty(props, "ping.interval.seconds", configFile);
      enablePing = Boolean.parseBoolean(props.getProperty("ping.enable", "true"));

    } catch (IOException e) {
      throw new RuntimeException("Failed to load configuration file: " + configFile, e);
    }
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

  public static String getEventSubscriptionsAddUrl() {
    return eventSubscriptionsAddUrl;
  }

  public static String getDevFlag() {
    return devFlag;
  }

  public static String getArbiterConfigJsonFile() {
    String filePath = ".\\" + arbiterConfigJsonFile;
    File file = new File(filePath);
    if (!file.exists()) {
      filePath = ".\\src\\main\\resources\\" + arbiterConfigJsonFile;
    }
    return filePath;
  }

  public static String getCalcSrvAbsoluteUrl() {
    return calcSrvUrl;
  }

  public static String getOikCertCrt() {
    return oikCertCrt;
  }

  public static boolean isTrustAll() {
    return isTrust;
  }

  public static String getPingInterval() {
    return pingInterval;
  }

  public static Boolean isEnablePing() {
    return enablePing;
  }
}
