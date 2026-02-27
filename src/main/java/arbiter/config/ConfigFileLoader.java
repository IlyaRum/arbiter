package arbiter.config;

import arbiter.data.UnitCollection;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;

import java.util.concurrent.CompletableFuture;

import static arbiter.constants.UnitCollectionConstants.CONFIG_KEY_OIK;
import static arbiter.constants.UnitCollectionConstants.CONFIG_KEY_PASSWORD;

public class ConfigFileLoader {

  private static final Logger logger = LoggerFactory.getLogger(ConfigFileLoader.class);

  private final Vertx vertx;

  public ConfigFileLoader(Vertx vertx) {
    this.vertx = vertx;
  }

  public CompletableFuture<Void> loadConfigFileAsync(String configFile, UnitCollection collection) {
    CompletableFuture<Void> initFuture = new CompletableFuture<>();
    vertx.executeBlocking(() -> {
        try {
          var buffer = vertx.fileSystem().readFileBlocking(configFile);

          JsonObject config = new JsonObject(buffer);
          JsonObject oikField = config.getJsonObject(CONFIG_KEY_OIK);
          if (oikField != null) {
            String newPassword = oikField.getString(CONFIG_KEY_PASSWORD);
            if (newPassword != null && !SecurityConfig.isEncoded(newPassword)) {
              String encodePassword = SecurityConfig.encodePassword(newPassword);
              logger.info("Encoded password: '" + encodePassword + "'");
              oikField.put(CONFIG_KEY_PASSWORD, encodePassword);
              Buffer updatedBuffer = Buffer.buffer(config.encodePrettily());
              vertx.fileSystem().writeFileBlocking(configFile, updatedBuffer);
              logger.info("Password in " + configFile + " has been overwritten successfully");
              collection.loadConfigData(updatedBuffer);
            } else {
              collection.loadConfigData(buffer);
            }
          }
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
