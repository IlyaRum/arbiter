package arbiter;

import arbiter.config.AppConfig;
import arbiter.di.DependencyInjector;
import arbiter.router.MainRouter;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

//mvn exec:java
//java -jar .\target\arbiter-1.0.0-SNAPSHOT-main.jar


public class MainVerticle extends AbstractVerticle {
  private HttpServer httpServer;
  private DependencyInjector dependencyInjector;
  @Override
  public void start(Promise<Void> startPromise) {

    // Инициализация зависимостей
    dependencyInjector = new DependencyInjector(vertx);

    // Создаем роутер
//    Router router = Router.router(vertx);
//
//    // Добавляем обработчик маршрута GET /
//    //curl -v http://localhost:8080
//    router.get("/").handler(routingContext -> {
//      routingContext.response()
//        .putHeader("content-type", "application/json")
//        .end("{\"message\": \"Service running...\"}");
//    });

    // Создание роутера
    MainRouter mainRouter = new MainRouter(
      vertx,
      dependencyInjector.getWebSocketController() // Добавляем WebSocket контроллер
    );

    Router router = mainRouter.createRouter();

    // Запускаем сервер
    httpServer  = vertx.createHttpServer();
    httpServer.requestHandler(router)
      .listen(AppConfig.HTTP_PORT)
      .onSuccess(server -> {
        System.out.println("HTTP server started on port " + AppConfig.HTTP_PORT);
        System.out.println("API available at: http://localhost:" + AppConfig.HTTP_PORT + AppConfig.API_PREFIX);
        System.out.println("WebSocket available at: ws://localhost:" + AppConfig.HTTP_PORT + AppConfig.API_PREFIX + AppConfig.WS_PATH);
        startPromise.complete();
      })
      .onFailure(failure -> {
        System.out.println("HTTP server started on port " + AppConfig.HTTP_PORT);
        startPromise.fail(failure);
        });
  }

  private Handler<RoutingContext> getRoutingContextHandler(RoutingContext ctx) {
    return routingContext -> {
      routingContext.response()
        .putHeader("content-type", "application/json")
        .end("{\"message\": \"Hello from Vert.x!\"}");
    };
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
