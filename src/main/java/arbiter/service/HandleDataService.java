package arbiter.service;

import arbiter.data.StoreData;
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
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HandleDataService extends ABaseService {

  private static final EventFormat JSON_FORMAT = EventFormatProvider.getInstance().resolveFormat(JsonFormat.CONTENT_TYPE);
  private static final Logger logger = LoggerFactory.getLogger(HandleDataService.class);

  private boolean firstTime = true;
  private String currentChannelId;
  private MeasurementDataProcessor measurementDataProcessor;
  private final ExecutorService executor;
  private CalculationServiceClient calculationClient;

  public HandleDataService(Vertx vertx, DependencyInjector dependencyInjector, WebClient webClient) {
    super(vertx);
    this.executor = Executors.newSingleThreadExecutor(r -> {
      Thread t = new Thread(r, "Data-Processing-Executor");
      t.setDaemon(true);
      return t;
    });

    this.calculationClient = new CalculationServiceClient(webClient, executor);
    this.measurementDataProcessor = new MeasurementDataProcessor(dependencyInjector, executor);
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

  private void handleProcessedData(StoreData storeData, String unitId) {
    if (storeData == null || storeData.size() == 0) {
      return;
    }

    executor.submit(() -> {
      try {
        if (firstTime) {
          String jsonPostData = convertStoreDataToJson(storeData);
          calculationClient.sendPostRequestAsync(jsonPostData);
          firstTime = false;
        } else if (unitId != null) {
          String jsonPutData = convertStoreDataToJson(storeData.getUnitDataList());
          calculationClient.sendPutRequestAsync(jsonPutData, unitId);
        }
      } catch (Exception e) {
        logger.error("Ошибка при обработке данных для отправки", e);
      }
    });
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

  public CalculationServiceClient getCalculationClient() {
    return calculationClient;
  }

  public void setCalculationClient(CalculationServiceClient calculationClient) {
    this.calculationClient = calculationClient;
  }
}
