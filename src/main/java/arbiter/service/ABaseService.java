package arbiter.service;

import io.vertx.core.Future;
import io.vertx.core.Vertx;

public abstract class ABaseService {

  protected final Vertx vertx;

  protected ABaseService(Vertx vertx) {
    this.vertx = vertx;
  }

  protected <T> Future<T> failedFuture(Throwable throwable) {
    return Future.failedFuture(throwable);
  }

  protected <T> Future<T> succeededFuture(T result) {
    return Future.succeededFuture(result);
  }

  public abstract void stop();
}
