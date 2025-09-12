package arbiter;

import arbiter.config.AppConfig;
import arbiter.di.DependencyInjector;
import arbiter.router.MainRouter;
import arbiter.service.SubscriptionService;
import arbiter.service.WebSocketService;
import data.DataFromCK11;
import data.UnitCollection;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

//mvn exec:java
//java -jar .\target\arbiter-1.0.0-SNAPSHOT-main.jar

public class MainVerticle extends AbstractVerticle {
  private HttpServer httpServer;
  private DependencyInjector dependencyInjector;

  @Override
  public void start(Promise<Void> startPromise) {

    AppConfig.loadConfig();

    UnitCollection data = new UnitCollection(vertx, AppConfig.ARBITER_CONFIG_FILE, "1.0.0");
    DataFromCK11 dataFromCK11 = new DataFromCK11();
    dependencyInjector = new DependencyInjector(vertx);

    WebSocketService webSocketService = new WebSocketService(vertx);
    SubscriptionService subscriptionService = new SubscriptionService(vertx);

    CompletableFuture<String> tokenFuture = webSocketService.getAndValidateToken();

    // Сохраняем token в переменную, доступную для обоих thenCompose
    CompletableFuture<String> tokenHolder = tokenFuture.thenApply(token -> token);

    CompletableFuture<JsonObject> resultFuture = tokenFuture
      .thenCompose(token -> {
        // Используем Future.toCompletionStage() для преобразования Future в CompletableFuture
        Future<JsonObject> jsonObjectFuture = webSocketService.connectToWebSocketServer(token);

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


    CompletableFuture<JsonObject> subscriptionFuture = resultFuture
      .thenCompose(jsonObject ->
        tokenHolder.thenCompose(token -> {
          System.out.println("Получен jsonObject для канала: " + jsonObject);
          String channelId = jsonObject.getString("subject");

          return subscriptionService.createSubscription(channelId, token)
            .toCompletionStage()
            .toCompletableFuture();
        })
      );

    // Обработка результата создания подписки
    subscriptionFuture.handle((subscriptionResult, subscriptionError) -> {
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

    CompletableFuture<JsonObject> changeSubscription = subscriptionFuture
      .thenCompose(jsonObject ->
        tokenHolder.thenCompose(token -> {
            System.out.println("Получен jsonObject для subscriptionFuture: " + jsonObject);
            String channelId = jsonObject.getString("subject");
          JsonObject valueObject = jsonObject.getJsonObject("value");
          String subscriptionId = valueObject.getString("subscriptionId");

            return subscriptionService.changeSubscription(channelId, subscriptionId, data.getUIDs(), null, token)
              .toCompletionStage().toCompletableFuture();
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

//    try {
//      JsonObject changeSubscription = changeSubscription.get(); // блокирует поток
//      System.out.println("Получен changeSubscription: " + changeSubscription);
//    } catch (InterruptedException | ExecutionException e) {
//      System.err.println("Ошибка при получении JSON: " + e.getMessage());
//    }

//    changeSubscription.thenApply(jsonObject -> {
//      System.out.println("Получен changeSubscription: " + jsonObject);
//      return jsonObject; // или выполните другие операции
//    });
//
//    changeSubscription
//      .thenAccept(result -> {
//        System.out.println("WebSocket connected successfully: " + result);
//      })
//      .exceptionally(error -> {
//        System.err.println("Failed to connect to WebSocket: " + error.getMessage());
//        return null;
//      });




//    MainRouter mainRouter = new MainRouter(
//      vertx,
//      dependencyInjector.getWebSocketController(),
//      dependencyInjector.getMonitoringController(),
//      dependencyInjector.getSubscriptionController()
//    );

//    Router router = mainRouter.createRouter();
    Router router = Router.router(vertx);
    // Запускаем сервер
    httpServer  = vertx.createHttpServer();
    httpServer.requestHandler(router);
    httpServer.listen(AppConfig.HTTP_PORT)
      .onSuccess(server -> {
        System.out.println("HTTP server started on port " + AppConfig.HTTP_PORT);
        System.out.println("WebSocket available at: http://localhost:" + AppConfig.HTTP_PORT + AppConfig.CORE_PREFIX + AppConfig.CHANNELS_OPEN);
        System.out.println("Add subscription available at: http://localhost:" + AppConfig.HTTP_PORT + AppConfig.MEASUREMENT_PREFIX + AppConfig.ADD_SUBSCRIPTION_BY_CHANNELID);
        System.out.println("Change subscription available at: http://localhost:" + AppConfig.HTTP_PORT + AppConfig.MEASUREMENT_PREFIX + AppConfig.CHANGE_SUBSCRIPTION);
        System.out.println("Delete subscription available at: http://localhost:" + AppConfig.HTTP_PORT + AppConfig.MEASUREMENT_PREFIX + AppConfig.DELETE_SUBSCRIPTION);
        System.out.println("Из " + AppConfig.ARBITER_CONFIG_FILE + " получен cрез из " + data.getUIDs().size() + " UID's " + data.getUIDs());
        startPromise.complete();
      })
      .onFailure(failure -> {
        System.out.println("HTTP server started on port " + AppConfig.HTTP_PORT);
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
