package arbiter.controller;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;

public abstract class ABaseController {

  protected final Vertx vertx;

  public ABaseController(Vertx vertx) {
    this.vertx = vertx;
  }

  public abstract void registerRoutes(Router router);

}
