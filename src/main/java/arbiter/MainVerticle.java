package arbiter;

import arbiter.config.AppConfig;
import arbiter.di.DependencyInjector;
import arbiter.router.MainRouter;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

//mvn exec:java
//java -jar .\target\arbiter-1.0.0-SNAPSHOT-main.jar

public class MainVerticle extends AbstractVerticle {
  private HttpServer httpServer;
  private DependencyInjector dependencyInjector;


  private static String authBasicCredentials;
  private String authTokenUrl;
  @Override
  public void start(Promise<Void> startPromise) {

    loadConfig();

    dependencyInjector = new DependencyInjector(vertx);

    // Создание роутера
    MainRouter mainRouter = new MainRouter(
      vertx,
      dependencyInjector.getWebSocketController(), // Добавляем WebSocket контроллер
      dependencyInjector.getMonitoringController()
    );

    Router router = mainRouter.createRouter();

    // Запускаем сервер
    httpServer  = vertx.createHttpServer();
    httpServer.requestHandler(router)
      .listen(AppConfig.HTTP_PORT)
      .onSuccess(server -> {
        System.out.println("HTTP server started on port " + AppConfig.HTTP_PORT);
        System.out.println("API available at: http://localhost:" + AppConfig.HTTP_PORT + AppConfig.API_PREFIX);
        System.out.println("WebSocket available at: http://localhost:" + AppConfig.HTTP_PORT + AppConfig.API_PREFIX + AppConfig.WS_PATH);
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

  private void loadConfig() {
    // Чтение конфигурации из файла или системных свойств
    String configFile = System.getProperty("config.file", "config.properties");

    try {
      Properties props = new Properties();
      props.load(new FileInputStream(".\\src\\main\\resources\\" + configFile));

      authBasicCredentials = props.getProperty("auth.basic.credentials");
      authTokenUrl = props.getProperty("auth.token.url");

      if (authBasicCredentials == null) {
        throw new RuntimeException("Missing required properties in config file");
      }

    } catch (IOException e) {
      throw new RuntimeException("Failed to load configuration file: " + configFile, e);
    }
  }


  public static String getAuthBasicCredentials() {
    return authBasicCredentials;
  }
}
