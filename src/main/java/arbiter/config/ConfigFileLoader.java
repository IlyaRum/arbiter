package arbiter.config;

import arbiter.data.UnitCollection;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;

import java.util.concurrent.CompletableFuture;

/**
 * Обработчик процесса загрузки конфигурационного файла
 */
public class ConfigFileLoader {

  private static final Logger logger = LoggerFactory.getLogger(ConfigFileLoader.class);

  private final ConfigFileManager fileManager;
  private final ConfigPasswordEncoder passwordEncoder;
  private final Vertx vertx;

  public ConfigFileLoader(Vertx vertx) {
    this(vertx, new ConfigFileManager(vertx), new ConfigPasswordEncoder(new ConfigFileManager(vertx)));
  }

  public ConfigFileLoader(Vertx vertx, ConfigFileManager fileManager, ConfigPasswordEncoder passwordEncoder) {
    this.fileManager = fileManager;
    this.passwordEncoder = passwordEncoder;
    this.vertx = vertx;
  }

  public CompletableFuture<Void> loadConfigFileAsync(String configFile, UnitCollection collection) {
    CompletableFuture<Void> initFuture = new CompletableFuture<>();

    vertx.executeBlocking(() -> {
        try {
          JsonObject config = fileManager.readConfigFile(configFile);
          JsonObject processedConfig = passwordEncoder.processPasswordEncoding(configFile, config);
          Buffer buffer = fileManager.configToBuffer(processedConfig);
          collection.loadConfigData(buffer);
          return null;
        } catch (Exception e) {
          logger.error("Failed to load config: " + e.getMessage(), e);
          throw new RuntimeException("Config loading failed", e);
        }
      }, false)
      .onSuccess(v -> {
        logger.info("Config loaded successfully");
        initFuture.complete(null);
      })
      .onFailure(err -> {
        logger.error("Loading configFile failed: " + err.getMessage());
        initFuture.completeExceptionally(err);
        vertx.close().onComplete(v -> System.exit(1));
      });

    return initFuture;
  }
}
