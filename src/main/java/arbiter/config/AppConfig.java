package arbiter.config;

import io.vertx.core.json.JsonObject;

public class AppConfig {
  public static final int HTTP_PORT = 8080;
  public static final String API_PREFIX = "/api/v1";
  public static final String WS_PATH = "/channels/open";
  public static final String CLOUDEVENTS_PROTOCOL = "cloudevents.json";
  public static final String BEARER_PREFIX = "Bearer ";

  public static JsonObject getConfig() {
    return new JsonObject()
      .put("http.port", HTTP_PORT)
      .put("api.prefix", API_PREFIX)
      .put("ws.path", WS_PATH)
      .put("cloudevents.protocol", CLOUDEVENTS_PROTOCOL);
  }
}
