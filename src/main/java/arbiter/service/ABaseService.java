package arbiter.service;

import io.cloudevents.CloudEvent;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

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


  public void handleSuccess(RoutingContext ctx, int statusCode, String message) {
    try {
      //решение ошибки "Response head already sent"
      if (!ctx.response().ended()) {
        ctx.response()
          .setStatusCode(statusCode)
          .putHeader("Content-Type", "application/json")
          .end(message);
      }
    } catch (IllegalStateException e) {
      System.out.println("Ответ уже отправлен, игнорируем повторную отправку");
    }
  }

  public void handleError(RoutingContext ctx, Throwable throwable) {
    ctx.response()
      .setStatusCode(500)
      .putHeader("Content-Type", "application/json")
      .end(new JsonObject()
        .put("error", throwable.getMessage())
        .encode());
  }

  public void sendError(RoutingContext context, int statusCode, String message) {
    context.response()
      .setStatusCode(statusCode)
      .putHeader("Content-Type", "application/json")
      .end("{\"error\": \"" + message + "\"}");
  }

  public List<String> extractFromJsonArray(JsonArray jsonArray) {
    List<String> arrays = new ArrayList<>();
    if (jsonArray != null) {
      for (int i = 0; i < jsonArray.size(); i++) {
        String array = jsonArray.getString(i);
        if (array != null && !array.isEmpty()) {
          arrays.add(array);
        }
      }
    }
    return arrays;
  }

  static void logCloudEvent(CloudEvent cloudEvent) {
    System.out.println("\n=== ПОЛУЧЕНО CLOUDEVENT ===");
    System.out.println("specversion: " + cloudEvent.getSpecVersion());
    System.out.println("source: " + cloudEvent.getSource());
    System.out.println("type: " + cloudEvent.getType());
    System.out.println("id: " + cloudEvent.getId());
    System.out.println("time: " + cloudEvent.getTime());
    String subject = cloudEvent.getSubject();

    if (subject != null) {
      System.out.println("subject: " + subject);
    }

    if (cloudEvent.getData() != null) {
      String data = new String(cloudEvent.getData().toBytes(), StandardCharsets.UTF_8);
      System.out.println("Data: " + data);
    }
    System.out.println();
  }
}
