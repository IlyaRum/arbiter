package arbiter;

import arbiter.config.AppConfig;
import arbiter.di.DependencyInjector;
import arbiter.initialization.ApplicationInitializer;
import arbiter.initialization.SubscriptionManager;
import arbiter.initialization.WebSocketManager;
import arbiter.router.MainRouter;
import data.UnitCollection;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

//mvn exec:java
//java -jar .\target\arbiter-1.0.0-SNAPSHOT-main.jar

public class MainVerticle extends AbstractVerticle {
  private HttpServer httpServer;
  private DependencyInjector dependencyInjector;
  private static final Logger logger = LoggerFactory.getLogger(MainVerticle.class);

  @Override
  public void start(Promise<Void> startPromise) {
    try {
      ApplicationInitializer initializer = new ApplicationInitializer(vertx, logger);
      CompletableFuture<DependencyInjector> initFuture = initializer.initialize();

      initFuture.thenCompose(dependencyInjector -> {
          this.dependencyInjector = dependencyInjector;
          UnitCollection data = new UnitCollection(vertx, AppConfig.ARBITER_CONFIG_FILE, "1.0.0");
          WebSocketManager webSocketManager = new WebSocketManager(dependencyInjector, logger);
          SubscriptionManager subscriptionManager = new SubscriptionManager(dependencyInjector, logger, data);
          return startHttpServer(startPromise)
            .thenCompose(ignore -> {
              // После успешного запуска сервера инициализируем WebSocket
              logServerStartup(data);
              return initializeWebSocketFlow(dependencyInjector, webSocketManager, subscriptionManager);
            });
        })
        .exceptionally(throwable -> {
          handleStartupError(startPromise, throwable);
          return null;
        });

    } catch (Exception e) {
      handleStartupError(startPromise, e);
    }
  }

  private CompletionStage<Object> initializeWebSocketFlow(DependencyInjector dependencyInjector,
                                                          WebSocketManager webSocketManager,
                                                          SubscriptionManager subscriptionManager) {
    return dependencyInjector.getTokenService().getTokenAsync()
      .thenCompose(token -> {
        logger.info("Token obtained: " + token);
        return webSocketManager.connect(token)
          .thenCompose(channelId -> subscriptionManager.createSubscription(channelId, token))
          .thenCompose(subscriptionResult -> {
            JsonObject valueObject = subscriptionResult.getJsonObject("value");
            String subscriptionId = valueObject.getString("subscriptionId");
            logger.info("subscriptionId: " + subscriptionId);
            return subscriptionManager.changeSubscription(webSocketManager.getChannelId(), subscriptionId, token
            );
          });
      })
      .handle((result, error) -> {
        if (error != null) {
          logger.error("WebSocket initialization failed", error);
          throw new CompletionException(error);
        }
        logger.info("WebSocket flow initialized successfully");
        return null;
      });
  }


  private CompletableFuture<Void> startHttpServer(Promise<Void> startPromise) {
    MainRouter mainRouter = new MainRouter(
      vertx,
      dependencyInjector.getWebSocketController(),
      dependencyInjector.getMonitoringController(),
      dependencyInjector.getSubscriptionController()
    );

    Router router = mainRouter.createRouter();
    httpServer = vertx.createHttpServer();
    httpServer.requestHandler(router);

    return httpServer.listen(AppConfig.HTTP_PORT)
      .toCompletionStage()
      .toCompletableFuture()
      .thenAccept(server -> {
        startPromise.complete();
      });
  }

  private void logServerStartup(UnitCollection data) {
    logger.info("HTTP server started on port " + AppConfig.HTTP_PORT);
    System.out.println("HTTP server started on port " + AppConfig.HTTP_PORT);
    System.out.println("WebSocket available at: http://localhost:" + AppConfig.HTTP_PORT + AppConfig.CORE_PREFIX + AppConfig.CHANNELS_OPEN);
    System.out.println("Add subscription available at: http://localhost:" + AppConfig.HTTP_PORT + AppConfig.MEASUREMENT_PREFIX + AppConfig.ADD_SUBSCRIPTION_BY_CHANNELID);
    System.out.println("Change subscription available at: http://localhost:" + AppConfig.HTTP_PORT + AppConfig.MEASUREMENT_PREFIX + AppConfig.CHANGE_SUBSCRIPTION);
    System.out.println("Delete subscription available at: http://localhost:" + AppConfig.HTTP_PORT + AppConfig.MEASUREMENT_PREFIX + AppConfig.DELETE_SUBSCRIPTION);
    System.out.println("Force reconnect available at: http://localhost:" + AppConfig.HTTP_PORT + AppConfig.CORE_PREFIX + AppConfig.FORCE_RECONNECT);
    System.out.println("Stop reconnecting available at: http://localhost:" + AppConfig.HTTP_PORT + AppConfig.CORE_PREFIX + AppConfig.STOP_RECONNECTING);
    System.out.println("Reconnection stats available at: http://localhost:" + AppConfig.HTTP_PORT + AppConfig.RECONNECTION_STATS);
    System.out.println("Metrics available at: http://localhost:" + AppConfig.HTTP_PORT + AppConfig.METRICS);
    System.out.println("Info available at: http://localhost:" + AppConfig.HTTP_PORT + AppConfig.INFO);
    System.out.println("Из " + AppConfig.ARBITER_CONFIG_FILE + " получен cрез из " + data.getUIDs().size() + " UID's " + data.getUIDs());
  }

  private void handleStartupError(Promise<Void> startPromise, Throwable throwable) {
    logger.error("Application startup failed", throwable);
    startPromise.fail(throwable);
  }

  @Override
  public void stop(Promise<Void> stopPromise) {
    if (httpServer != null) {
      httpServer.close()
        .onSuccess(v -> stopPromise.complete())
        .onFailure(stopPromise::fail);
    } else {
      stopPromise.complete();
    }
  }
}
