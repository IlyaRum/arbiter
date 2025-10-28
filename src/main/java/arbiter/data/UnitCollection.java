package arbiter.data;

import arbiter.measurement.Measurement;
import io.vertx.core.Vertx;
import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

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
  //private List<String> UIDs = new ArrayList<>(); //срез uid'ов, которые добавляем набора значений измерений в подписке
  private static final Logger logger = LoggerFactory.getLogger(UnitCollection.class);

  private boolean writeEnable;
  private boolean debug;
  private boolean checkEvent;
  private String eventUID;
  private String writeEventUID;

  //  private WebSocketClient webSocketClient;
  private Vertx vertx;

//  private Consumer<List<Parameter>> processData;
//  private Consumer<Unit> processEvent;

//  public UnitCollection(Vertx vertx, String configFile, String versionInfo,
//                        String password, Consumer<List<Parameter>> processData,
//                        Consumer<Unit> processEvent) {
//    this.vertx = vertx;
//    this.version = versionInfo;
//    this.password = password;
//    this.processData = processData;
//    this.processEvent = processEvent;
//
//    loadConfig(configFile);
//  }

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
        this.eventUID = config.getString("изменение критерия МДП СМЗУ");
        this.writeEventUID = config.getString("запись критерия МДП СМЗУ");

        checkEvent = true;
        if (eventUID.isEmpty()) {
          checkEvent = false;
        } else {
          if (writeEventUID.isEmpty()) {
            checkEvent = false;
          }
        }

        // Загрузка units
        JsonArray unitsArray = config.getJsonArray("сечение");
        for (int i = 0; i < unitsArray.size(); i++) {
          this.unit = new Unit(i, unitsArray.getJsonObject(i));
          units.add(unit);
        }

        //connect();
      })
      .onFailure(err -> {
        //logger.error("Ошибка загрузки конфигурации", err);
        System.err.println("Ошибка загрузки конфигурации: " + err.getMessage());
      });
  }

//  public void addID(final String id) {
//    if (id != null && id.length() == 36) {
//      if (!UIDs.contains(id)) {
//        UIDs.add(id);
//      }
//    }
//  }

  public boolean yesNo(JsonObject obj, String key) {
    if (!obj.containsKey(key)) return false;
    return "да".equalsIgnoreCase(obj.getString(key));
  }

  public List<String> getUIDs() {
    List<String> parameterUIDs = units.stream()
      .flatMap(unit -> unit.getParameters().stream())
      .flatMap(parameter -> parameter.getUIDs().stream())
      .toList();

//    List<String> repairSchemaUIDs = units.stream()
//      .map(Unit::getRepairSchema)
//      .flatMap(repairSchema -> repairSchema.getRepairGroupValues().stream())
//      .flatMap(repairGroupValue -> repairGroupValue.getUIDs().stream())
//      .toList();

    List<String> repairSchemaUIDs = units.stream()
      .map(Unit::getRepairSchema)
      .filter(Objects::nonNull)
      .map(RepairSchema::getRepairGroupValues)
      .filter(Objects::nonNull)
      .flatMap(List::stream)
      .map(RepairGroupValue::getUIDs)
      .filter(Objects::nonNull)
      .flatMap(List::stream)
      .toList();

    List<String> topologyUIDs = units.stream()
      .flatMap(unit -> unit.getTopologies().stream())
      .flatMap(topology -> topology.getUIDs().stream())
      .toList();

    List<String> elementUIDs = units.stream()
      .flatMap(unit -> unit.getElements().stream())
      .flatMap(element -> element.getUIDs().stream())
      .toList();

    List<String> influencingFactorUIDs = units.stream()
      .flatMap(unit -> unit.getInfluencingFactors().stream())
      .flatMap(influencingFactor -> influencingFactor.getUIDs().stream())
      .toList();



    List<String> allUIDs = new ArrayList<>();
    allUIDs.addAll(parameterUIDs);
    allUIDs.addAll(topologyUIDs);
    allUIDs.addAll(repairSchemaUIDs);
    allUIDs.addAll(elementUIDs);
    allUIDs.addAll(influencingFactorUIDs);

    return allUIDs;
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

//  private void onDataReceived(JsonArray data) {
//    List<Parameter> updatedParams = new ArrayList<>();
//
//    for (int i = 0; i < data.size(); i++) {
//      JsonObject measurement = data.getJsonObject(i);
//      String id = measurement.getString("id").toLowerCase();
//      double value = measurement.getDouble("value");
//      Instant time = Instant.parse(measurement.getString("time"));
//      int qCode = measurement.getInteger("qCode", 0);
//
//      for (Unit unit : units) {
//        for (Parameter param : unit.getParameters().values()) {
//          if (param.getId().equalsIgnoreCase(id)) {
//            if (!param.isAssigned() ||
//              !param.getTime().equals(time) ||
//              param.getValue() != value) {
//
//              //param.setData(value, time, qCode);
//              updatedParams.add(param);
//
//              if (debug) {
//                //logger.debug("{} - {} = {} [{}] {}", unit.getName(), param.getName(), value, qCode, time);
//              }
//            }
//          }
//        }
//      }
//    }
//
//    if (!updatedParams.isEmpty()) {
//      //logger.info("Получено {} новых значений", updatedParams.size());
//      processData.accept(updatedParams);
//    }
//  }

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

//  public void addToWriteBuffer(Measurement measurement) {
//    writeBuffer.add(measurement);
//  }

  public List<Unit> getUnits() {
    return units;
  }
//  public boolean isWriteEnable() { return writeEnable; }
//  public Status getStatus() { return status; }

  public String getEventUID() {
    return eventUID;
  }

  /**
   * Определяет, нужно ли отслеживать события изменения критерия МДП СМЗУ в системе.
   */
  public boolean isCheckEvent() {
    return checkEvent;
  }


}



