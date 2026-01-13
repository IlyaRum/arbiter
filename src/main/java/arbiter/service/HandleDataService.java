package arbiter.service;


import arbiter.config.AppConfig;
import arbiter.data.*;
import arbiter.di.DependencyInjector;
import arbiter.measurement.MeasurementDataProcessor;
import arbiter.measurement.MeasurementList;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.cloudevents.CloudEvent;
import io.cloudevents.CloudEventData;
import io.cloudevents.core.format.EventFormat;
import io.cloudevents.core.provider.EventFormatProvider;
import io.cloudevents.jackson.JsonFormat;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HandleDataService extends ABaseService{

  private final ExecutorService executor = Executors.newSingleThreadExecutor();
  private static final EventFormat JSON_FORMAT = EventFormatProvider.getInstance().resolveFormat(JsonFormat.CONTENT_TYPE);
  private static final Logger logger = LoggerFactory.getLogger(HandleDataService.class);

  private final WebClient webClient;
  private boolean firstTime = true;
  private String currentChannelId;
  private MeasurementDataProcessor measurementDataProcessor;

  public HandleDataService(Vertx vertx, DependencyInjector dependencyInjector, WebClient webClient) {
    super(vertx);
    this.measurementDataProcessor = new MeasurementDataProcessor(dependencyInjector);
    this.webClient = webClient;
    this.measurementDataProcessor.setDataReadyCallback(this::handleProcessedData);
  }

  public HandleDataService(Vertx vertx, DependencyInjector dependencyInjector) {
    this(vertx, dependencyInjector, WebClient.create(vertx));
  }


  public Handler<String> handleTextMessage(Promise<JsonObject> promise) {
    return message -> {
      try {
        logger.info("Input message: " + message);

        EventFormat format = EventFormatProvider.getInstance().resolveFormat(JsonFormat.CONTENT_TYPE);
        CloudEvent event = format.deserialize(message.getBytes(StandardCharsets.UTF_8));

        //logCloudEvent(event);

        String eventType = event.getType();
        //logger.debug("eventType: " + eventType);
        if (eventType.equals("ru.monitel.ck11.channel.opened.v2")) {
          handleChannelOpened(event);
          // Завершаем promise только при получении сообщения об открытии
          if (!promise.future().isComplete()) {
            String data = cloudEventToString(event);
            JsonObject jsonData = new JsonObject(data);
            promise.tryComplete(jsonData);
          }
        } else if (eventType.equals("ru.monitel.ck11.measurement-values.data.v2")) {
          //тип для подписки на актуальные данные: ru.monitel.ck11.measurement-values.data.v2;
          handleMeasurementData(event);
          //TODO[IER] здесь нужно будет сохранить в объект полученные данные
        } else if (eventType.startsWith("ru.monitel.ck11.rt-events.")) {
          //события реального времени
          handleRTEvents(event);
          //TODO[IER] здесь нужно реализовать полученные данные из эвента
        } else if (eventType.equals("ru.monitel.ck11.events.stream-started.v2")) {
          logger.info("подписка на события стартовала");
        } else if (eventType.equals("ru.monitel.ck11.events.stream-broken.v2")) {
          logger.info("подписка на события остановлена");
          //close();
        }
      } catch (Exception e) {
        logger.error("Ошибка парсинга CloudEvent: " + e.getMessage());
        logger.info("Полученное сообщение: " + message);
        promise.tryFail(e);
      }
    };
  }

  private void handleMeasurementData(CloudEvent event) {
    //logger.debug("[data.v2]event: " + event);
    CloudEventData cloudEventData = event.getData();
    //logAsync("[data.v2]cloudEventData: " + cloudEventData);

    assert cloudEventData != null;
    String jsonData = cloudEventData.toString();
    String jsonStr = jsonData
      .replace("JsonCloudEventData{node=", "")
      .replace("}}", "}");

    JsonObject data = new JsonObject(jsonStr);
    JsonArray dataArray = data.getJsonArray("data");
    MeasurementList measurementList = new MeasurementList();

    for (int i = 0; i < dataArray.size(); i++) {
      JsonObject item = dataArray.getJsonObject(i);
      JsonNode jsonNode = item.mapTo(JsonNode.class);
      measurementList.add(jsonNode);
    }

    measurementDataProcessor.onDataReceived(measurementList);
  }

  private void handleRTEvents(CloudEvent event) {
    logger.debug("[rt-events]event: " + event);
    CloudEventData cloudEventData = event.getData();
    //logAsync("[rt-events]CloudEventData: " + cloudEventData);
  }

  private void handleChannelOpened(CloudEvent event) {
    logger.debug("[channel.opened]event: " + event);
    logAsync("[channel.opened]event: " + event);
    currentChannelId = event.getSubject();
  }

  public static String cloudEventToString(CloudEvent event) {
    byte[] bytes = JSON_FORMAT.serialize(event);
    return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
  }

  private void handleProcessedData(StoreData data, String unitId) {
    if (data != null && data.size() > 0) {

      String jsonData = convertStoreDataToJson(Collections.singletonList(data.getUnitDataList()));

      if (isFirstTime()) {
        sendPostRequestAsync(jsonData);
        setFirstTime(false);
      } else {
        sendPutRequestAsync(jsonData, unitId);
      }
    }
  }

  public void sendPostRequestAsync(String jsonData) {
    executor.submit(() -> {
      try {
        sendPostRequest(jsonData);
      } catch (Exception e) {
        logger.error("Ошибка при асинхронной отправке данных", e);
      }
    });
  }

  public void sendPutRequestAsync(String jsonData, String unitId) {
    executor.submit(() -> {
      try {
        sendPutRequest(jsonData, unitId);
      } catch (Exception e) {
        logger.error("Ошибка при асинхронной отправке PUT данных", e);
      }
    });
  }

  private void sendPutRequest(String jsonData, String unitId) {
    logger.debug(String.format("Отправляем PUT запрос для сечения '%s'", unitId));

    webClient.putAbs(AppConfig.getCalcSrvAbsoluteUrl())
      .putHeader("Content-Type", "application/json")
      .sendBuffer(Buffer.buffer(jsonData))
      .compose(response -> {
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
          logger.debug("Данные успешно обновлены. Ответ: " + response.bodyAsString());
          return Future.succeededFuture();
        } else {
          return Future.failedFuture("HTTP error: " + response.statusCode() + " - " + response.bodyAsString());
        }
      })
      .onSuccess(v -> logger.debug("PUT запрос выполнен успешно"))
      .onFailure(err -> logger.error("Ошибка при отправке PUT запроса: " + err.getMessage()));
  }

  private void sendPostRequest(String jsonData) {
    logger.info("Отправляем POST запрос в арбитр расчетов с данными: " + jsonData);

    webClient.postAbs(AppConfig.getCalcSrvAbsoluteUrl())
      .putHeader("Content-Type", "application/json")
      .sendBuffer(Buffer.buffer(jsonData))
      .compose(response -> {
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
          logger.debug("Данные успешно отправлены. Ответ: " + response.bodyAsString());
          return Future.succeededFuture();
        } else {
          return Future.failedFuture("HTTP error: " + response.statusCode() + " - " + response.bodyAsString());
        }
      })
      .onSuccess(v -> logger.debug("POST запрос выполнен успешно"))
      .onFailure(err -> logger.error("Ошибка при отправке POST запроса: " + err.getMessage()));
  }

  private String convertStoreDataToJson(Object objects) {
    try {
      ObjectMapper mapper = new ObjectMapper();
      mapper.registerModule(JsonFormat.getCloudEventJacksonModule());
      mapper.registerModule(new JavaTimeModule());
      mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
      mapper.enable(SerializationFeature.INDENT_OUTPUT);

      return mapper.writeValueAsString(objects);

    } catch (Exception e) {
      logger.error("Ошибка при конвертации данных в JSON: " + e.getMessage());
      return "{\"error\": \"" + e.getMessage() + "\"}";
    }
  }

  //для решения WARNING: Thread vert.x-eventloop-thread-1 has been blocked for 769173 ms, time limit is 2000 ms
  private void logAsync(String message) {
    vertx.executeBlocking(() -> {
        System.out.println("----");
        System.out.println(message);
        System.out.println();
        return null;
      }, false)
      .onFailure(err -> {
        System.err.println("Logging failed: " + err.getMessage());
      });
  }

  public String getCurrentChannelId() {
    return currentChannelId;
  }

  public boolean isFirstTime() {
    return firstTime;
  }

  public void setFirstTime(boolean firstTime) {
    this.firstTime = firstTime;
  }

  public void setMeasurementDataProcessor(MeasurementDataProcessor measurementDataProcessor) {
    this.measurementDataProcessor = measurementDataProcessor;
  }
}
