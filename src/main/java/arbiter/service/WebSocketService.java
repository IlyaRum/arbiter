package arbiter.service;

import arbiter.config.AppConfig;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.*;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public class WebSocketService extends ABaseService {
  private final WebSocketClient webSocketClient;
  private final AtomicReference<WebSocket> currentWebSocket = new AtomicReference<>();
  private CompletableFuture<JsonObject> connectionResponseFuture;
  private String currentChannelId;


  public WebSocketService(Vertx vertx) {
    super(vertx);
    this.webSocketClient = vertx.createWebSocketClient(new WebSocketClientOptions()
        .setSsl(false)
        .setTrustAll(true)
        .setVerifyHost(false)
        .setLogActivity(true)
      //.setTryUsePerFrameCompression(true)
      //.setTryUsePerMessageCompression(true)
    );
  }

  public Future<JsonObject> connectToWebSocketServer(String token) {
    Promise<JsonObject> promise = Promise.promise();

    WebSocketConnectOptions options = new WebSocketConnectOptions()
      .setAbsoluteURI("wss://ia-oc-w-aiptst.cdu.so/api/public/core/v2.1/channels/open")
      .addSubProtocol(AppConfig.CLOUDEVENTS_PROTOCOL)
      .setPort(443)
      .addHeader("authorization", "Bearer " + token);

    webSocketClient
      .connect(options)
      .onComplete(res -> {
        if (res.succeeded()) {
          WebSocket webSocket = res.result();
          webSocket.textMessageHandler(message -> {

            try {
              JsonObject receivedJson = new JsonObject(message);
              System.out.println(receivedJson.encodePrettily());

            } catch (Exception e) {
              System.out.println("Не удалось распарсить JSON: " + e.getMessage());
            }
          });

          webSocket.closeHandler(v -> {
            System.out.println("WebSocket connection closed");
          });

          // Обработка ошибок
          webSocket.exceptionHandler(error -> {
            promise.fail("WebSocket ошибка: " + error.getMessage());
            webSocket.close((short)1011, "Server error");
          });

          promise.complete();
        }
      })
      .onSuccess(webSocket -> {
        System.out.println("WebSocket соединение установлено успешно!");
      })
      .onFailure(error -> {
        String fullUri = buildUriFromOptions(options);
        System.err.println("Ошибка подключения к " + fullUri + ": " + error.getMessage());
        promise.fail("Ошибка подключения: " + error.getMessage());
      });

    return promise.future();
  }

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
            }
          } catch (Exception e) {
            sendError(context, 500, "Invalid token response format");
          }
        } else {
          sendError(context, response.statusCode(), "Token request failed: " + response.bodyAsString());
        }
      })
      .onFailure(err -> {
        sendError(context, 500, "Token service unavailable: " + err.getMessage());
      });
  }
  private String buildUriFromOptions(WebSocketConnectOptions options) {
    String protocol = "wss";
    String host = options.getHost();
    int port = options.getPort();
    String path = options.getURI();

    // Стандартные порты можно не указывать
    if ((port == 443) || (port == 80)) {
      return protocol + "://" + host + path;
    } else {
      return protocol + "://" + host + ":" + port + path;
    }
  }


  // Обработка CloudEvent сообщения
  private static void processCloudEvent(JsonObject cloudEvent) {
    String id = cloudEvent.getString("id");
    String source = cloudEvent.getString("source");
    String type = cloudEvent.getString("type");
    String specVersion = cloudEvent.getString("specversion");
    JsonObject data = cloudEvent.getJsonObject("data");

    System.out.println("Обработка CloudEvent:");
    System.out.println("ID: " + id);
    System.out.println("Source: " + source);
    System.out.println("Type: " + type);
    System.out.println("Spec Version: " + specVersion);
    if (data != null) {
      System.out.println("Data: " + data);
    }
  }

  private static void sendTestCloudEvent(WebSocket webSocket) {
    JsonObject cloudEvent = new JsonObject()
      .put("specversion", "1.0")
      .put("type", "com.example.test")
      .put("source", "urn:example:test-client")
      .put("id", "test-event-" + System.currentTimeMillis())
      .put("time", java.time.Instant.now().toString())
      .put("datacontenttype", "application/json")
      .put("data", new JsonObject()
        .put("message", "Тестовое сообщение")
        .put("timestamp", System.currentTimeMillis()));

    webSocket.writeTextMessage(cloudEvent.toString());
    System.out.println("Отправлено тестовое CloudEvent сообщение");
  }


  private Future<JsonObject> waitForConnectionResponse(WebSocket webSocket) {
    return Future.future(promise -> {
      // Устанавливаем таймаут для ожидания ответа
      vertx.setTimer(10000, timerId -> {
        if (!promise.future().isComplete()) {
          promise.fail("Timeout waiting for connection response");
          if (webSocket != null && !webSocket.isClosed()) {
            webSocket.close((short) 1008, "Connection timeout");
          }
        }
      });

      // Ждем завершения future с ответом
      connectionResponseFuture.whenComplete((response, throwable) -> {
        if (throwable != null) {
          promise.fail(throwable);
        } else {
          promise.complete(response);
        }
      });
    });
  }

  private void setupWebSocketHandlers(WebSocket webSocket) {
    // Обработчик входящих текстовых сообщений
    webSocket.textMessageHandler(message -> {
      handleIncomingMessage(message);
    });

    // Обработчик входящих бинарных сообщений
    webSocket.binaryMessageHandler(buffer -> {
      try {
        String message = buffer.toString();
        handleIncomingMessage(message);
      } catch (Exception e) {
        System.err.println("Failed to parse binary message: " + e.getMessage());
      }
    });

    // Обработчик закрытия соединения
    webSocket.closeHandler(v -> {
      System.out.println("WebSocket connection closed");
      if (connectionResponseFuture != null && !connectionResponseFuture.isDone()) {
        connectionResponseFuture.completeExceptionally(new RuntimeException("Connection closed before response"));
      }
      currentWebSocket.set(null);
      currentChannelId = null;
    });

    // Обработчик исключений
    webSocket.exceptionHandler(throwable -> {
      System.err.println("WebSocket error: " + throwable.getMessage());
      if (connectionResponseFuture != null && !connectionResponseFuture.isDone()) {
        connectionResponseFuture.completeExceptionally(throwable);
      }
      currentWebSocket.set(null);
      currentChannelId = null;
    });

    // Обработчик pong (для поддержания соединения)
    webSocket.pongHandler(pong -> {
      System.out.println("Received pong from server");
    });
  }

  private void handleIncomingMessage(String message) {
    try {
      JsonObject cloudEvent = new JsonObject(message);
      System.out.println("Received message: " + cloudEvent.encodePrettily());

      // Проверяем, это ли ожидаемый ответ на подключение
      if (isConnectionResponse(cloudEvent)) {
        handleConnectionResponse(cloudEvent);
      } else {
        // Обрабатываем другие сообщения
        handleOtherMessages(cloudEvent);
      }

    } catch (Exception e) {
      System.err.println("Invalid JSON message: " + message);
      System.err.println("Error: " + e.getMessage());
    }
  }

  private boolean isConnectionResponse(JsonObject cloudEvent) {
    return "ru.monitel.ck11.channel.opened.v2".equals(cloudEvent.getString("type")) &&
      "1.0".equals(cloudEvent.getString("specversion")) &&
      cloudEvent.containsKey("subject");
  }

  private void handleConnectionResponse(JsonObject cloudEvent) {
    if (connectionResponseFuture != null && !connectionResponseFuture.isDone()) {
      currentChannelId = cloudEvent.getString("subject");

      System.out.println("WebSocket connection established successfully");
      System.out.println("Channel ID: " + currentChannelId);
      System.out.println("Connection response: " + cloudEvent.encodePrettily());

      // Завершаем future с ответом от сервера
      connectionResponseFuture.complete(cloudEvent);
    }
  }

  private void handleOtherMessages(JsonObject cloudEvent) {
    String type = cloudEvent.getString("type");
    String subject = cloudEvent.getString("subject", "");

    System.out.println("Received message - Type: " + type + ", Subject: " + subject);

    // Здесь можно добавить обработку различных типов сообщений
    switch (type) {
      case "ru.monitel.ck11.message.received.v1":
        handleMessageReceived(cloudEvent);
        break;
      case "ru.monitel.ck11.channel.closed.v1":
        handleChannelClosed(cloudEvent);
        break;
      case "ru.monitel.ck11.error.v1":
        handleErrorEvent(cloudEvent);
        break;
      default:
        System.out.println("Unknown message type: " + type);
    }
  }

  private void handleMessageReceived(JsonObject cloudEvent) {
    JsonObject data = cloudEvent.getJsonObject("data");
    if (data != null) {
      System.out.println("Message received: " + data.encodePrettily());
    }
  }

  private void handleChannelClosed(JsonObject cloudEvent) {
    String reason = cloudEvent.getString("reason", "No reason provided");
    System.out.println("Channel closed by server. Reason: " + reason);
    closeConnection();
  }

  private void handleErrorEvent(JsonObject cloudEvent) {
    JsonObject errorData = cloudEvent.getJsonObject("data");
    if (errorData != null) {
      String errorCode = errorData.getString("code", "UNKNOWN_ERROR");
      String errorMessage = errorData.getString("message", "No error message provided");
      System.err.println("Server error: " + errorCode + " - " + errorMessage);
    }
  }

  public Future<Void> sendMessage(JsonObject cloudEvent) {
    WebSocket webSocket = currentWebSocket.get();
    if (webSocket == null || webSocket.isClosed()) {
      return Future.failedFuture("WebSocket not connected");
    }

    try {
      // Добавляем обязательные поля если их нет
      if (!cloudEvent.containsKey("specversion")) {
        cloudEvent.put("specversion", "1.0");
      }
      if (!cloudEvent.containsKey("id")) {
        cloudEvent.put("id", UUID.randomUUID().toString());
      }
      if (!cloudEvent.containsKey("time")) {
        cloudEvent.put("time", Instant.now().toString());
      }
      if (!cloudEvent.containsKey("source")) {
        cloudEvent.put("source", "vertx-client");
      }

      String message = cloudEvent.encode();
      webSocket.writeTextMessage(message);

      System.out.println("Message sent: " + cloudEvent.getString("type", "unknown"));
      return Future.succeededFuture();

    } catch (Exception e) {
      return Future.failedFuture("Failed to send message: " + e.getMessage());
    }
  }

  public Future<Void> closeConnection() {
    WebSocket webSocket = currentWebSocket.get();
    if (webSocket != null) {
      return webSocket.close((short) 1000, "Normal closure");
    }
    return Future.succeededFuture();
  }

  public boolean isConnected() {
    WebSocket webSocket = currentWebSocket.get();
    return webSocket != null && !webSocket.isClosed();
  }

  public String getCurrentChannelId() {
    return currentChannelId;
  }

  public WebSocket getCurrentWebSocket() {
    return currentWebSocket.get();
  }

  @Override
  public void stop() {
    if (isConnected()) {
      closeConnection();
    }
    if (webSocketClient != null) {
      webSocketClient.close();
    }
  }
}
