package arbiter.service;

import arbiter.config.AppConfig;
import io.cloudevents.CloudEvent;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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

  @Deprecated
  public void getAndValidateToken(RoutingContext context) {
    // Специальные настройки для обхода SSL
    HttpClientOptions options = new HttpClientOptions()
      .setSsl(true)
      .setTrustAll(true) //отключает проверку сертификатов
      .setVerifyHost(false); //Отключает проверку hostname

    WebClient insecureClient = WebClient.wrap(context.vertx().createHttpClient(options));

    // Вызываем эндпоинт для получения токена
    insecureClient.postAbs(AppConfig.getAuthTokenUrl())
      .putHeader("Authorization", "Basic " + AppConfig.getAuthBasicCredentials())
      .putHeader("Content-Type", "application/json")
      .send()
      .onSuccess(response -> {
        if (response.statusCode() == 200) {
          try {
            JsonObject tokenResponse = response.bodyAsJsonObject();
            String token = tokenResponse.getString("access_token"); // предполагаемое поле с токеном

            if (token != null && !token.isEmpty()) {
              context.put("authToken", token);
              context.next();
            } else {
              sendError(context, 401, "Token not found in response");
              System.err.println("Token not found in response");
            }
          } catch (Exception e) {
            sendError(context, 500, "Invalid token response format");
            System.err.println("Invalid token response format");
          }
        } else {
          sendError(context, response.statusCode(), "Token request to " + AppConfig.getAuthTokenUrl() + " failed");
          System.err.println("Token request to " + AppConfig.getAuthTokenUrl() + " failed");
        }
      })
      .onFailure(err -> {
        sendError(context, 500, "Token service unavailable: " + err.getMessage());
        System.err.println("Token service unavailable: " + err.getMessage());
      });
  }

  public CompletableFuture<String> getAndValidateToken() {
    CompletableFuture<String> future = new CompletableFuture<>();

    HttpClientOptions options = new HttpClientOptions()
      .setSsl(true)
      .setTrustAll(true)
      .setVerifyHost(false);

    //TODO лучше создать WebClient один раз и переиспользовать его
    WebClient insecureClient = WebClient.wrap(vertx.createHttpClient(options));

    insecureClient.postAbs(AppConfig.getAuthTokenUrl())
      .putHeader("Authorization", "Basic " + AppConfig.getAuthBasicCredentials())
      .putHeader("Content-Type", "application/json")
      .send()
      .onSuccess(response -> {
        try {
          if (response.statusCode() == 200) {
            JsonObject tokenResponse = response.bodyAsJsonObject();
            String token = tokenResponse.getString("access_token");

            if (token != null && !token.isEmpty()) {
              future.complete(token);
            } else {
              future.completeExceptionally(new RuntimeException("Token not found in response"));
            }
          } else {
            future.completeExceptionally(new RuntimeException("HTTP " + response.statusCode() + ": " + response.statusMessage()));
          }
        } catch (Exception e) {
          future.completeExceptionally(new RuntimeException("Failed to parse token response", e));
        }
      })
      .onFailure(err -> {
        future.completeExceptionally(new RuntimeException("Token service unavailable", err));
      });

    return future;
  }

  public List<String> extractUidsFromJsonArray(JsonArray jsonArray) {
    List<String> uids = new ArrayList<>();
    if (jsonArray != null) {
      for (int i = 0; i < jsonArray.size(); i++) {
        String uid = jsonArray.getString(i);
        if (uid != null && !uid.isEmpty()) {
          uids.add(uid);
        }
      }
    }
    return uids;
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
