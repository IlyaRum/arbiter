package arbiter.data;

import arbiter.data.dto.CommonFieldDto;
import arbiter.data.model.*;
import arbiter.measurement.Measurement;
import io.vertx.core.Vertx;
import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class UnitCollection {
  private String version;
  private String oik;
  private String user;
  private String password;
  private Status status = Status.DISCONNECTED;
  private Unit unit;
  private JsonObject config;

  private CommonField commonField;
  private List<Unit> units = new CopyOnWriteArrayList<>();
  private List<Measurement> writeBuffer = new CopyOnWriteArrayList<>();
  //private List<String> UIDs = new ArrayList<>(); //срез uid'ов, которые добавляем набора значений измерений в подписке
  private static final Logger logger = LoggerFactory.getLogger(UnitCollection.class);

  private boolean writeEnable;
  private boolean skipCycle;
  private boolean minusHK;
  private boolean debug;
  private boolean checkEvent;
  private String eventUID;
  private String writeEventUID;

  //сторож
  private String heartBeatUID;
  private Integer heartBeatInterval;
  private boolean watchDogWait = false;

  private Vertx vertx;
  private final Map<String, Set<String>> unitTargetUids = new ConcurrentHashMap<>();

  public UnitCollection(Vertx vertx, String configFile, String versionInfo) {
    this.vertx = vertx;
    this.version = versionInfo;

    this.commonField = new CommonField();

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

        JsonObject oikField = config.getJsonObject("ОИК");
        this.oik = oikField.getString("адрес");
        this.user = oikField.getString("пользователь", "");
        this.password = oikField.getString("пароль", "");
        this.debug = yesNo(oikField, "отладка");

        this.writeEnable = yesNo(config, "запись в ОИК");
        this.eventUID = config.getString("изменение критерия МДП СМЗУ");
        this.writeEventUID = config.getString("запись критерия МДП СМЗУ");

        this.skipCycle = yesNo(config, "не проверять данные при старте 2 цикла");
        this.minusHK = yesNo(config, "вычитать НК");

        JsonObject hasWriteHeartBeat = config.getJsonObject("сторож");
        if(hasWriteHeartBeat != null) {
          this.heartBeatUID = hasWriteHeartBeat.getString("id");
          this.heartBeatInterval = hasWriteHeartBeat.getInteger("интервал", 60);
          this.watchDogWait = yesNo(hasWriteHeartBeat, "ждать");
        }

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

        // Инициализация целевых UID после загрузки units
        initializeUnitTargetUids();
        logger.info("Unit target UIDs initialized for " + unitTargetUids.size() + " units");

        initializeCommonFields();

        //connect();
      })
      .onFailure(err -> {
        //logger.error("Ошибка загрузки конфигурации", err);
        System.err.println("Ошибка загрузки конфигурации: " + err.getMessage());
      });
  }

  public boolean yesNo(JsonObject obj, String key) {
    if (!obj.containsKey(key)) return false;
    return "да".equalsIgnoreCase(obj.getString(key));
  }

//  public int getInteger(JsonObject obj, String key, int defaultValue) {
//    if (!obj.containsKey(key)) return defaultValue;
//    return obj.getInteger(key);
//  }

  public List<String> getUIDs() {
    List<String> allUIDs = new ArrayList<>();
    allUIDs.addAll(getParameterUIDs());
    allUIDs.addAll(getTopologyUIDs());
    allUIDs.addAll(getCompositionUIDs());
    allUIDs.addAll(getElementUIDs());
    allUIDs.addAll(getInfluencingFactorUIDs());
    return allUIDs;
  }

  private List<String> getInfluencingFactorUIDs() {
    return units.stream()
      .flatMap(unit1 -> unit1.getInfluencingFactors().stream())
      .flatMap(influencingFactor -> influencingFactor.getUIDs().stream())
      .toList();
  }

  private List<String> getElementUIDs() {
    return units.stream()
      .flatMap(unit1 -> unit1.getElements().stream())
      .flatMap(element -> element.getUIDs().stream())
      .toList();
  }

  private List<String> getTopologyUIDs() {
    return units.stream()
      .flatMap(unit1 -> unit1.getTopologies().stream())
      .flatMap(topology -> topology.getUIDs().stream())
      .toList();
  }

  private List<String> getCompositionUIDs() {
    return units.stream()
      .map(Unit::getRepairSchema)
      .filter(Objects::nonNull)
      .map(RepairSchema::getRepairGroupValues)
      .filter(Objects::nonNull)
      .flatMap(Collection::stream)
      .map(RepairGroupValue::getValues)
      .filter(Objects::nonNull)
      .flatMap(Collection::stream)
      .map(Composition::getUIDs)
      .flatMap(Collection::stream)
      .toList();
  }

  private List<String> getParameterUIDs() {
    return units.stream()
      .flatMap(unit -> unit.getParameters().stream())
      .flatMap(parameter -> parameter.getUIDs().stream())
      .toList();
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

  /**
   * Инициализирует целевые UID для всех юнитов
   */
  private void initializeUnitTargetUids() {
    for (Unit unit : units) {
      Set<String> targetUids = extractTargetUidsFromUnit(unit);
      unitTargetUids.put(unit.getName(), targetUids);
      logger.info("Initialized target UIDs for unit " + unit.getName() + ": " + targetUids);
    }
  }

  public CommonField getCommonField() {
    return commonField;
  }

  public CommonFieldDto getCommonFieldDto() {
    return new CommonFieldDto(commonField);
  }

  private void initializeCommonFields() {
    commonField.setOikAddress(oik);
    commonField.setUser(user);
    commonField.setPassword(password);
    commonField.setDebug(debug);
    commonField.setWriteEnable(writeEnable);
    commonField.setEventUID(eventUID);
    commonField.setWriteEventUID(writeEventUID);
    commonField.setSkipCycle(skipCycle);
    commonField.setHeartBeatUID(heartBeatUID);
    commonField.setHeartBeatInterval(heartBeatInterval);
    commonField.setWatchDogWait(watchDogWait);
    commonField.setMinusHK(minusHK);
  }

  /**
   * Извлекает целевые UID из исходных данных юнита
   */
  private Set<String> extractTargetUidsFromUnit(Unit unit) {
    Set<String> targetUids = new HashSet<>();
    List<Parameter> parameters = unit.getParameters();

    for (Parameter param : parameters) {
      String paramName = param.getName();
      if (isTargetParameter(paramName)) {
        targetUids.add(param.getId().toLowerCase());
      }
    }
    return targetUids;
  }

  /**
   * Проверяет, является ли параметр целевым для отслеживания
   */
  private boolean isTargetParameter(String parameterName) {
    if (parameterName == null) return false;
    return parameterName.equals("МДП без ПА [СМЗУ]") ||
      parameterName.equals("МДП с ПА [СМЗУ]") ||
      parameterName.equals("АДП [СМЗУ]") ||
      parameterName.equals("Номер цикла расчета СМЗУ");
  }

  /**
   * Возвращает целевые UID для указанного юнита
   */
  public Set<String> getTargetUidsForUnit(String unitName) {
    return unitTargetUids.getOrDefault(unitName, Collections.emptySet());
  }

  /**
   * Возвращает целевые UID для указанного юнита
   */
  public Set<String> getTargetUidsForUnit(Unit unit) {
    return getTargetUidsForUnit(unit.getName());
  }

  /**
   * Получает UID параметра "Номер цикла расчета СМЗУ" из юнита
   */
  public String getCycleNumberUidFromUnit(Unit unit) {
    List<Parameter> parameters = unit.getParameters();

    for (Parameter param : parameters) {
      String paramName = param.getName();
      if (paramName != null && paramName.equals("Номер цикла расчета СМЗУ")) {
        return param.getId().toLowerCase();
      }
    }
    return null;
  }

  /**
   * Проверяет, инициализированы ли целевые UID для всех юнитов
   */
  public boolean areTargetUidsInitialized() {
    return !unitTargetUids.isEmpty();
  }

}



