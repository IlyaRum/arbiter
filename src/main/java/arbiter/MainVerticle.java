package arbiter;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

//mvn exec:java


public class MainVerticle extends AbstractVerticle {

  @Override
  public void start(Promise<Void> startPromise) {

    // Создаем роутер
    Router router = Router.router(vertx);

    // Добавляем обработчик маршрута GET /
    //curl -v http://localhost:8080
    router.get("/").handler(routingContext -> {
      routingContext.response()
        .putHeader("content-type", "application/json")
        .end("{\"message\": \"Hello from Vert.x!\"}");
    });

    // Запускаем сервер
    vertx.createHttpServer()
      .requestHandler(router)
      .listen(8080)
      .onSuccess(server -> {
        System.out.println("Server started on port 8080");
        startPromise.complete();
      })
      .onFailure(startPromise::fail);
  }

  private Handler<RoutingContext> getRoutingContextHandler(RoutingContext ctx) {
    return routingContext -> {
      routingContext.response()
        .putHeader("content-type", "application/json")
        .end("{\"message\": \"Hello from Vert.x!\"}");
    };
  }
}
