package arbiter;

import arbiter.config.AppConfig;
import arbiter.di.DependencyInjector;
import arbiter.router.MainRouter;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;

//mvn exec:java
//java -jar .\target\arbiter-1.0.0-SNAPSHOT-main.jar

public class MainVerticle extends AbstractVerticle {
  private HttpServer httpServer;
  private DependencyInjector dependencyInjector;

  @Override
  public void start(Promise<Void> startPromise) {

    AppConfig.loadConfig();

    dependencyInjector = new DependencyInjector(vertx);

    MainRouter mainRouter = new MainRouter(
      vertx,
      dependencyInjector.getWebSocketController(),
      dependencyInjector.getMonitoringController(),
      dependencyInjector.getSubscriptionController()
    );

    Router router = mainRouter.createRouter();

    // Запускаем сервер
    httpServer  = vertx.createHttpServer();
    httpServer.requestHandler(router)
      .listen(AppConfig.HTTP_PORT)
      .onSuccess(server -> {
        System.out.println("HTTP server started on port " + AppConfig.HTTP_PORT);
        System.out.println("API available at: http://localhost:" + AppConfig.HTTP_PORT + AppConfig.CORE_PREFIX);
        System.out.println("WebSocket available at: http://localhost:" + AppConfig.HTTP_PORT + AppConfig.CORE_PREFIX + AppConfig.WS_PATH);
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
