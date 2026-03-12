package arbiter.config;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;

/**
 * Менеджер для работы с файлами конфигурации.
 */
public class ConfigFileManager {

  private static final Logger logger = LoggerFactory.getLogger(ConfigFileLoader.class);

  private final Vertx vertx;

  public ConfigFileManager(Vertx vertx) {
    this.vertx = vertx;
  }

  public JsonObject readConfigFile(String configFile) {
    try {
      Buffer buffer = vertx.fileSystem().readFileBlocking(configFile);
      return new JsonObject(buffer);
    } catch (Exception e) {
      logger.error("Failed to load config: " + e.getMessage(), e);
      throw new RuntimeException("Config loading failed", e);
    }
  }

  public void writeConfigFile(String configFile, JsonObject config) {
    try {
      Buffer buffer = Buffer.buffer(config.encodePrettily());
      vertx.fileSystem().writeFileBlocking(configFile, buffer);
      logger.info("Config file written successfully: " + configFile);
    } catch (Exception e) {
      logger.error("Failed to write config file: " + configFile, e);
      throw new RuntimeException("Config file writing failed", e);
    }
  }

  public Buffer configToBuffer(JsonObject config) {
    return Buffer.buffer(config.encodePrettily());
  }
}
