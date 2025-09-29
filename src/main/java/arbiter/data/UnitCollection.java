package data;

import arbiter.service.WebSocketService;
import io.vertx.core.Vertx;
import io.vertx.core.http.WebSocketClient;
import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import measurement.Measurement;
import measurement.MeasurementList;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class UnitCollection {
  private String version;
  private String oik;
  private String user;
  private String password;
  private Status status = Status.DISCONNECTED;
  private Unit unit;
  private JsonObject config;

  private List<Unit> units = new CopyOnWriteArrayList<>();
  private List<Measurement> writeBuffer = new CopyOnWriteArrayList<>();
  private List<String> UIDs = new ArrayList<>(); //срез uid'ов, которые добавляем набора значений измерений в подписке
  private static final Logger logger = LoggerFactory.getLogger(UnitCollection.class);

  private boolean writeEnable;
  private boolean debug;
  private boolean checkEvent;
  private String eventUID;

  private WebSocketClient webSocketClient;
  private Vertx vertx;

  private Consumer<List<Parameter>> processData;
  private Consumer<Unit> processEvent;

  public UnitCollection(Vertx vertx, String configFile, String versionInfo,
                        String password, Consumer<List<Parameter>> processData,
                        Consumer<Unit> processEvent) {
    this.vertx = vertx;
    this.version = versionInfo;
    this.password = password;
    this.processData = processData;
    this.processEvent = processEvent;

    loadConfig(configFile);
  }

  public UnitCollection(Vertx vertx, String configFile, String versionInfo) {
    this.vertx = vertx;
    this.version = versionInfo;

    loadConfigAsync(configFile);
  }

  private void loadConfigAsync(String configFile) {
    this.vertx.executeBlocking(() -> {
        loadConfig(configFile);
        return null;
      }, false)
      .onFailure(err -> {
        System.err.println("Loading configFile failed: " + err.getMessage());
      });
  }

  private void loadConfig(String configFile) {
    vertx.fileSystem().readFile(configFile)
      .onSuccess(buffer -> {
        this.config = new JsonObject(buffer);
        this.oik = config.getJsonObject("ОИК").getString("адрес");
        this.user = config.getJsonObject("ОИК").getString("пользователь", "");
        this.debug = yesNo(config.getJsonObject("ОИК"), "отладка");
        this.writeEnable = yesNo(config, "запись в ОИК");

        // Загрузка units
        JsonArray unitsArray = config.getJsonArray("сечение");
        for (int i = 0; i < unitsArray.size(); i++) {
          this.unit = new Unit(i, unitsArray.getJsonObject(i), this);
          units.add(unit);
        }

        //connect();
      })
      .onFailure(err -> {
        //logger.error("Ошибка загрузки конфигурации", err);
        System.err.println("Ошибка загрузки конфигурации: " +  err.getMessage());
      });
  }

  public void addID(final String id) {
    if (id != null && id.length() == 36) {
      if (!UIDs.contains(id)) {
        UIDs.add(id);
      }
    }
  }

  public boolean yesNo(JsonObject obj, String key) {
    if (!obj.containsKey(key)) return false;
    return "да".equalsIgnoreCase(obj.getString(key));
  }

  public List<String> getUIDs() {
    return UIDs;
  }

  //  public void connect() {
//    status = TStatus.DISCONNECTED;
//    int attempts = 0;
//    int maxReconnect = config.getInteger("количество попыток соединения с ОИК", 100);
//
//    vertx.setPeriodic(1000, timerId -> {
//      if (status != TStatus.CONNECTED && attempts < maxReconnect) {
//        attempts++;
//        tryConnect();
//      } else if (attempts >= maxReconnect) {
//        status = TStatus.STOPPED;
//        vertx.cancelTimer(timerId);
//      }
//    });
//  }

//  private void tryConnect() {
//    ck11Client = new CK11Client(vertx, oik, user, password, debug);
//
//    ck11Client.connect()
//      .compose(token -> {
//        webSocketClient = new CK11WebSocketClient(vertx, oik, token);
//        return webSocketClient.connect();
//      })
//      .compose(webSocket -> {
//        status = TStatus.CONNECTED;
//        setupWebSocketHandlers(webSocket);
//        return initialRead();
//      })
//      .onSuccess(v -> {
//        logger.info("Успешное подключение к ОИК: {}", oik);
//      })
//      .onFailure(throwable -> {
//        logger.error("Ошибка подключения", throwable);
//        status = TStatus.DISCONNECTED;
//      });
//  }

//  private void setupWebSocketHandlers(WebSocket webSocket) {
//    webSocket.textMessageHandler(message -> {
//      JsonObject json = new JsonObject(message);
//      if (json.containsKey("data")) {
//        onDataReceived(json.getJsonArray("data"));
//      } else if (json.containsKey("event")) {
//        onEventReceived(json.getJsonObject("event"));
//      }
//    });
//
//    webSocket.closeHandler(v -> {
//      //logger.warn("WebSocket соединение закрыто");
//      connect(); // Переподключение
//    });
//
//    // Пинг
//    vertx.setPeriodic(30000, id -> {
//      webSocket.writePing(Buffer.buffer("ping"));
//    });
//  }

  private void onDataReceived(JsonArray data) {
    List<Parameter> updatedParams = new ArrayList<>();

    for (int i = 0; i < data.size(); i++) {
      JsonObject measurement = data.getJsonObject(i);
      String id = measurement.getString("id").toLowerCase();
      double value = measurement.getDouble("value");
      Instant time = Instant.parse(measurement.getString("time"));
      int qCode = measurement.getInteger("qCode", 0);

      for (Unit unit : units) {
        for (Parameter param : unit.getParameters().values()) {
          if (param.getId().equalsIgnoreCase(id)) {
            if (!param.isAssigned() ||
              !param.getTime().equals(time) ||
              param.getValue() != value) {

              //param.setData(value, time, qCode);
              updatedParams.add(param);

              if (debug) {
                //logger.debug("{} - {} = {} [{}] {}", unit.getName(), param.getName(), value, qCode, time);
              }
            }
          }
        }
      }
    }

    if (!updatedParams.isEmpty()) {
      //logger.info("Получено {} новых значений", updatedParams.size());
      processData.accept(updatedParams);
    }
  }

//  private void onEventReceived(JsonObject event) {
//    JsonObject data = event.getJsonObject("data");
//    String id = data.getString("id");
//    String uid = data.getJsonArray("parameters").getJsonObject(0).getString("uuid");
//
//    for (TUnit unit : units) {
//      if (unit.getEventObject().equalsIgnoreCase(uid)) {
//        logger.info("{} получено событие ({}): {}",
//          unit.getName(), id, data.getString("message"));
//
//        unit.setEventData(data);
//        processEvent.accept(unit);
//      }
//    }
//  }

//  public Future<Void> initialRead() {
//    List<String> ids = new ArrayList<>();
//    for (TUnit unit : units) {
//      for (TResult result : unit.getResults().values()) {
//        if (result.writable()) {
//          ids.add(result.getId());
//        }
//      }
//    }
//
//    return ck11Client.getValues(ids, 0)
//      .onSuccess(measurements -> {
//        for (Measurement measurement : measurements) {
//          for (TUnit unit : units) {
//            for (TResult result : unit.getResults().values()) {
//              if (result.getId().equalsIgnoreCase(measurement.getUid())) {
//                result.setValue(measurement.getValue(), measurement.getTime());
//              }
//            }
//          }
//        }
//      });
//  }

//  public Future<Void> write() {
//    if (writeBuffer.isEmpty()) {
//      return Future.succeededFuture();
//    }
//
//    List<Measurement> toWrite = new ArrayList<>(writeBuffer);
//    writeBuffer.clear();
//
//    return ck11Client.write(toWrite)
//      .onFailure(throwable -> {
//        logger.error("Ошибка записи", throwable);
//        writeBuffer.addAll(toWrite); // Возвращаем обратно в буфер
//        connect(); // Переподключаемся
//      });
//  }

  public void addToWriteBuffer(Measurement measurement) {
    writeBuffer.add(measurement);
  }

  // Геттеры
  public List<Unit> getUnits() { return units; }
  public boolean isWriteEnable() { return writeEnable; }
  public Status getStatus() { return status; }


  //тут получаем данные из СК-11 и сохраняем их
  public void onDataReceived(MeasurementList list) {
    StoreData result = new StoreData();

    try {
      for (int i = 0; i < list.size(); i++) {
        Measurement measurement = list.get(i);
        MemoryData memoryData = createMemoryData(measurement);

        //store.put(memoryData.getId(), memoryData);

        // Items[j].Parameters.Data[k]
        processParameters(memoryData, result);
      }

      if (result.size() > 0) {
        logger.info(String.format("### получено %d новых значений", result.size()));
        //dataProcessor.accept(result);

      }

    } catch (Exception e) {
      logger.error("Ошибка при обработке данных измерений", e);
    }
  }

  private MemoryData createMemoryData(Measurement measurement) {
    String id = measurement.getUid();
    double value = measurement.getValue();
    Instant time = measurement.getTimeStampAsInstant();
    int qCode = measurement.getQCode();

    return new MemoryData(id, value, time, qCode);
  }

  private void processParameters(MemoryData memoryData, StoreData result) {

    // Проходим по всем юнитам
    for (int j = 0; j < units.size(); j++) {
      Unit unit = units.get(j);
      Map<String, Parameter> parameters = unit.getParameters();

      // Проходим по всем параметрам юнитам
      // Аналог: for k := 0 to Items[j].Parameters.Count - 1 do
      for (Parameter parameter : parameters.values()) {

        // Сравниваем ID (case-insensitive)
        // Аналог: if CompareText(P.Id, Data.Id) = 0 then
        if (parameter.getId().equalsIgnoreCase(memoryData.getId())) {

          // Проверяем, изменились ли данные
          // Аналог: if not P.Assigned or (P.Time <> Data.Time) or (P.Value <> Data.Value) then
          if (parameter.isDataDifferent(memoryData.getValue(), memoryData.getTime())) {

            // Обновляем данные параметра
            // Аналог: P.SetData(Data.Value, Data.Time, Data.QCode)
            parameter.setData(memoryData.getValue(), memoryData.getTime(), memoryData.getQCode());
            result.add(parameter);
          }
          logger.debug(String.format("%s - %s = %f [%s] %s",
            unit.getName(), parameter.getName(), memoryData.getValue(),
            Integer.toHexString(memoryData.getQCode()),
            memoryData.getTime().toString()));
          break;
        }
      }
    }
  }
}



