package arbiter.initialization;

import arbiter.config.AppConfig;
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
    DependencyInjector dependencyInjector = new DependencyInjector(vertx);
    return CompletableFuture.completedFuture(dependencyInjector);
  }
}
