package arbiter.initialization;

import arbiter.config.AppConfig;
import arbiter.data.UnitCollection;
import arbiter.di.DependencyInjector;
import io.vertx.core.Vertx;
import io.vertx.core.internal.logging.Logger;

import java.util.concurrent.CompletableFuture;

public class ApplicationInitializer {
  private final Vertx vertx;
  private final Logger logger;

  public ApplicationInitializer(Vertx vertx, Logger logger) {
    this.vertx = vertx;
    this.logger = logger;
  }

  public CompletableFuture<DependencyInjector> initialize() {
    logger.info("------------------------------");
    logger.info("Starting Vert.x application...");
    AppConfig.loadConfig();
    UnitCollection unitCollection = new UnitCollection(vertx, AppConfig.getArbiterConfigJsonFile(), "1.0.0");
    return unitCollection.getInitFuture()
      .thenApply(v -> new DependencyInjector(vertx, unitCollection));
  }
}
