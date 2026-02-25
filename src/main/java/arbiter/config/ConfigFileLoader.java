package arbiter.config;

import arbiter.data.UnitCollection;
import io.vertx.core.Vertx;
import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;

import java.util.concurrent.CompletableFuture;

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
