package arbiter;

import io.vertx.core.Vertx;

public class AppLauncher {
  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(new MainVerticle());
  }
}
