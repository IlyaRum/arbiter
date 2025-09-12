package arbiter.service;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;

import java.util.concurrent.CompletableFuture;

public class TokenService {
  private final WebClient insecureClient;
  private final String authTokenUrl;
  private final String authBasicCredentials;

  public TokenService(Vertx vertx, String authTokenUrl, String authBasicCredentials) {
    HttpClientOptions options = new HttpClientOptions()
      .setSsl(true)
      .setTrustAll(true)
      .setVerifyHost(false);

    this.insecureClient = WebClient.wrap(vertx.createHttpClient(options));
    this.authTokenUrl = authTokenUrl;
    this.authBasicCredentials = authBasicCredentials;
  }

  public CompletableFuture<String> getTokenAsync() {
    CompletableFuture<String> future = new CompletableFuture<>();

    insecureClient.postAbs(authTokenUrl)
      .putHeader("Authorization", "Basic " + authBasicCredentials)
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
            future.completeExceptionally(new RuntimeException("HTTP " + response.statusCode()));
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

  public void getTokenForContext(RoutingContext context) {
    getTokenAsync()
      .thenAccept(token -> {
        context.put("authToken", token);
        context.next();
      })
      .exceptionally(throwable -> {
        sendError(context, 500, "Token service error: " + throwable.getMessage());
        System.err.println("Token service error: " + throwable.getMessage());
        return null;
      });
  }

  private void sendError(RoutingContext context, int statusCode, String message) {
    context.response()
      .setStatusCode(statusCode)
      .putHeader("Content-Type", "application/json")
      .end(new JsonObject().put("error", message).encode());
  }
}
