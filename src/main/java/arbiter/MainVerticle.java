package arbiter;

import arbiter.config.AppConfig;
import arbiter.di.DependencyInjector;
import arbiter.router.MainRouter;
import data.UnitCollection;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

//mvn exec:java
//java -jar .\target\arbiter-1.0.0-SNAPSHOT-main.jar

public class MainVerticle extends AbstractVerticle {
  private HttpServer httpServer;
  private DependencyInjector dependencyInjector;

  private static final Logger logger = LoggerFactory.getLogger(MainVerticle.class);

  @Override
  public void start(Promise<Void> startPromise) {
    logger.info("------------------------------");
    logger.info("Starting Vert.x application...");
    AppConfig.loadConfig();

    UnitCollection data = new UnitCollection(vertx, AppConfig.ARBITER_CONFIG_FILE, "1.0.0");
    dependencyInjector = new DependencyInjector(vertx);

    CompletableFuture<String> tokenFuture = dependencyInjector.getTokenService().getTokenAsync();

    // Сохраняем token в переменную, доступную для обоих thenCompose
    CompletableFuture<String> tokenHolder = tokenFuture
      .thenApply(token -> {
        logger.info("token: " + token);
        return token;
      });

    CompletableFuture<JsonObject> openChannelFuture = tokenFuture
      .thenCompose(token -> {
        Future<JsonObject> jsonObjectFuture = dependencyInjector.getWebSocketService()
          .connectToWebSocketServer(token);

        jsonObjectFuture.onComplete(ar -> {
          if (ar.succeeded()) {
            JsonObject result = ar.result();

            // Работаем с result
            System.out.println("Получен объект jsonObjectFuture : " + result);
          } else {
            // Обработка ошибки
            System.out.println("Ошибка: " + ar.cause().getMessage());
          }
        });
        return jsonObjectFuture.toCompletionStage();
      })
      .handle((result, error) -> {
        if (error != null) {
          System.err.println("Failed to connect to WebSocket: " + error.getMessage());
          throw new CompletionException(error);
        } else {
          System.out.println("WebSocket connected successfully: " + result);
          return result;
        }
      });

    CompletableFuture<String> channelIdFuture = openChannelFuture
      .thenApply(jsonObject -> {
        String channelId = jsonObject.getString("subject");
        System.out.println("Получен jsonObject для канала: " + jsonObject);
        logger.info("channelId: " + channelId);
        return channelId;
      });

    CompletableFuture<JsonObject> createSubscription = channelIdFuture
      .thenCompose(channelId ->
        tokenHolder.thenCompose(token -> dependencyInjector.getSubscriptionService().createSubscription(channelId, token)
          .toCompletionStage()
          .toCompletableFuture())
      );

    // Обработка результата создания подписки
    createSubscription.handle((subscriptionResult, subscriptionError) -> {
      if (subscriptionError != null) {
        System.err.println("Failed to create subscription: " + subscriptionError.getMessage());
        throw new CompletionException(subscriptionError);
      } else {
        System.out.println("Subscription created successfully: " + subscriptionResult);

        // WebSocket соединение остается открытым для приема сообщений
        System.out.println("WebSocket connection remains open for incoming messages");
        return subscriptionResult;
      }
    });

    CompletableFuture<JsonObject> changeSubscription = createSubscription
      .thenCompose(jsonObject ->
        tokenHolder.thenCompose(token -> {
            System.out.println("Получен jsonObject для createSubscription: " + jsonObject);
            JsonObject valueObject = jsonObject.getJsonObject("value");
            String subscriptionId = valueObject.getString("subscriptionId");
            logger.info("subscriptionId: " + subscriptionId);

            return channelIdFuture.thenCompose(channelId ->
              dependencyInjector.getSubscriptionService()
                .changeSubscription(channelId, subscriptionId, data.getUIDs(), null, token)
                .toCompletionStage()
                .toCompletableFuture());
          }
        ));

    changeSubscription.handle((result, error) -> {
        if (error != null) {
          System.err.println("Failed to create changeSubscription: " + error.getMessage());
          throw new CompletionException(error);
        } else {
          System.out.println("ChangeSubscription connected successfully: " + result);

          // WebSocket соединение остается открытым для приема сообщений
          System.out.println("WebSocket connection remains open for incoming messages");
          return result;
        }
      });

    MainRouter mainRouter = new MainRouter(
      vertx,
      dependencyInjector.getWebSocketController(),
      dependencyInjector.getMonitoringController(),
      dependencyInjector.getSubscriptionController()
    );

    Router router = mainRouter.createRouter();
    httpServer  = vertx.createHttpServer();
    httpServer.requestHandler(router);
    httpServer.listen(AppConfig.HTTP_PORT)
      .onSuccess(server -> {
        logger.info("HTTP server started on port " + AppConfig.HTTP_PORT);
        System.out.println("HTTP server started on port " + AppConfig.HTTP_PORT);
        System.out.println("WebSocket available at: http://localhost:" + AppConfig.HTTP_PORT + AppConfig.CORE_PREFIX + AppConfig.CHANNELS_OPEN);
        System.out.println("Add subscription available at: http://localhost:" + AppConfig.HTTP_PORT + AppConfig.MEASUREMENT_PREFIX + AppConfig.ADD_SUBSCRIPTION_BY_CHANNELID);
        System.out.println("Change subscription available at: http://localhost:" + AppConfig.HTTP_PORT + AppConfig.MEASUREMENT_PREFIX + AppConfig.CHANGE_SUBSCRIPTION);
        System.out.println("Delete subscription available at: http://localhost:" + AppConfig.HTTP_PORT + AppConfig.MEASUREMENT_PREFIX + AppConfig.DELETE_SUBSCRIPTION);
        System.out.println("Из " + AppConfig.ARBITER_CONFIG_FILE + " получен cрез из " + data.getUIDs().size() + " UID's " + data.getUIDs());
        startPromise.complete();
      })
      .onFailure(failure -> {
        logger.error("HTTP server started on port " + AppConfig.HTTP_PORT);
        startPromise.fail(failure);
        });
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
