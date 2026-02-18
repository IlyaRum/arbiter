package arbiter.data;

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

import static arbiter.constants.UnitCollectionConstants.*;
import static arbiter.util.ConfigValidator.*;

public class UnitCollection {
  private String version;
  private String oikAddress;
  private String oikUser;
  private String oikPassword;
  private boolean oikDebug;
  private Status status = Status.DISCONNECTED;
  private Unit unit;
  private JsonObject config;
  private boolean configValid = false;

  private CommonField commonField;
  private List<Unit> units = new CopyOnWriteArrayList<>();
  private List<Measurement> writeBuffer = new CopyOnWriteArrayList<>();
  private static final Logger logger = LoggerFactory.getLogger(UnitCollection.class);

  private boolean writeEnable;
  private boolean skipCycle;
  private boolean minusHK;
  private boolean checkEvent;
  private String eventUID;
  private String writeEventUID;
  private String instance;
  private Integer eventDelta;
  private Integer port;
  private Integer requestDelay;
  private Integer connectAttempt;
  private Integer oikConnectTimeout;

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
        logger.error("Loading configFile failed: " + err.getMessage());
        vertx.close().onComplete(v -> {
          System.exit(1);
        });
      });
  }

  private void loadConfig(String configFile) {
    vertx.fileSystem().readFile(configFile)
      .onSuccess(buffer -> {
        try {
        this.config = new JsonObject(buffer);

        JsonObject oikField = (JsonObject) validateFieldNameAndValue(config.getJsonObject(CONFIG_KEY_OIK), CONFIG_KEY_OIK);
        this.oikAddress = (String) validateFieldNameAndValue(oikField.getString(CONFIG_KEY_ADDRESS), CONFIG_KEY_OIK);
        this.oikUser = (String) validateFieldNameAndValue(oikField.getString(CONFIG_KEY_USER), CONFIG_KEY_USER);
        this.oikPassword = (String) validateFieldName(oikField.getString(CONFIG_KEY_PASSWORD), CONFIG_KEY_PASSWORD);
        this.oikDebug = yesNo(oikField, CONFIG_KEY_DEBUG);

        this.writeEnable = yesNo(config, CONFIG_KEY_WRITE_ENABLE);
        this.eventUID = validateFieldNameAndValueUuid(CONFIG_KEY_EVENT_UID, config);
        this.writeEventUID = validateFieldNameAndValueUuid(CONFIG_KEY_WRITE_EVENT_UID, config);
        this.instance = (String) validateFieldName(config.getString(CONFIG_KEY_INSTANCE), CONFIG_KEY_INSTANCE);

        this.skipCycle = yesNo(config, CONFIG_KEY_SKIP_CYCLE);
        this.minusHK = yesNo(config, CONFIG_KEY_MINUS_HK);
        this.eventDelta = validateFieldName(config, CONFIG_KEY_EVENT_DELTA, config.getInteger(CONFIG_KEY_EVENT_DELTA, 0));
        this.port = validateFieldName(config, CONFIG_KEY_PORT, config.getInteger(CONFIG_KEY_PORT, 8080));
        this.requestDelay = validateFieldName(config, CONFIG_KEY_REQUEST_DELAY, config.getInteger(CONFIG_KEY_REQUEST_DELAY, 1000));

        this.connectAttempt = validateFieldName(config, CONFIG_KEY_CONNECT_ATTEMPT, config.getInteger(CONFIG_KEY_CONNECT_ATTEMPT, 100));
        this.oikConnectTimeout = validateFieldName(config, CONFIG_KEY_OIK_CONNECT_TIMEOUT, config.getInteger(CONFIG_KEY_OIK_CONNECT_TIMEOUT, 1000));

        JsonObject hasWriteHeartBeat = (JsonObject) validateFieldNameAndValue(config.getJsonObject(CONFIG_KEY_WATCHDOG), CONFIG_KEY_WATCHDOG);
        if (hasWriteHeartBeat != null) {
          this.heartBeatUID = validateFieldNameAndValueUuidInSection(CONFIG_KEY_HEARTBEAT_ID, hasWriteHeartBeat, CONFIG_KEY_WATCHDOG);
          this.heartBeatInterval = validateFieldName(hasWriteHeartBeat, CONFIG_KEY_HEARTBEAT_INTERVAL, hasWriteHeartBeat.getInteger(CONFIG_KEY_HEARTBEAT_INTERVAL, 60));
          this.watchDogWait = yesNo(hasWriteHeartBeat, CONFIG_KEY_WATCHDOG_WAIT);
        }

        if (eventUID != null) {
          checkEvent = true;
          if (eventUID.isEmpty()) {
            checkEvent = false;
          } else {
            if (writeEventUID.isEmpty()) {
              checkEvent = false;
            }
          }
        }

        JsonArray unitsArray = (JsonArray) validateFieldNameAndValue(config.getJsonArray(CONFIG_KEY_UNITS_ARRAY), CONFIG_KEY_UNITS_ARRAY);
          for (int i = 0; i < unitsArray.size(); i++) {
            this.unit = new Unit(i, unitsArray.getJsonObject(i));
            units.add(unit);
          }

        initializeCommonFields();

        //connect();

      } catch (Exception e) {
      handleConfigError("Ошибка при обработке конфигурационного файла: " + e.getMessage(), e);
    }
      })
      .onFailure(err -> {
        handleConfigError("Ошибка при загрузке конфигурационного файла: " + err.getMessage(), err);
      });
  }

  private void handleConfigError(String errorMsg, Throwable err) {
    logger.error(errorMsg, err);
    Runtime.getRuntime().halt(1);
  }

  public boolean yesNo(JsonObject obj, String key) {
    return "да".equalsIgnoreCase(validateBooleanValue((String) validateFieldName(obj.getString(key), key), key));
  }

  public List<String> getUIDs() {
    List<String> allUIDs = new ArrayList<>();
    allUIDs.addAll(getParameterUIDs());
    allUIDs.addAll(getTopologyUIDs());
    allUIDs.addAll(getCompositionUIDs());
    allUIDs.addAll(getElementUIDs());
    allUIDs.addAll(getInfluencingFactorUIDs());
    allUIDs.addAll(getArpmUIDs());
    allUIDs.addAll(getUnitResultUids());
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

  private List<String> getArpmUIDs() {
    return units.stream()
      .map(Unit::getArpmList)
      .flatMap(Collection::stream)
      .map(ARPM::getParameterArpm)
      .flatMap(Collection::stream)
      .map(ParameterArpm::getUIDs)
      .flatMap(Collection::stream)
      .toList();
  }

  private List<String> getUnitResultUids() {
    return units.stream()
      .flatMap(unit -> unit.getUnitResults().stream())
      .flatMap(ur -> ur.getUIDs().stream())
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

  private void initializeCommonFields() {
    commonField.setOikAddress(oikAddress);
    commonField.setUser(oikUser);
    commonField.setPassword(oikPassword);
    commonField.setDebug(oikDebug);
    commonField.setWriteEnable(writeEnable);
    commonField.setEventUID(eventUID);
    commonField.setWriteEventUID(writeEventUID);
    commonField.setSkipCycle(skipCycle);
    commonField.setHeartBeatUID(heartBeatUID);
    commonField.setHeartBeatInterval(heartBeatInterval);
    commonField.setWatchDogWait(watchDogWait);
    commonField.setMinusHK(minusHK);
    commonField.setEventDelta(eventDelta);
    commonField.setPort(port);
    commonField.setRequestDelay(requestDelay);
    commonField.setConnectAttempt(connectAttempt);
    commonField.setOikConnectTimeout(oikConnectTimeout);
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

  /**
   * Проверяет, валидна ли загруженная конфигурация
   */
  public boolean isConfigValid() {
    return configValid;
  }

}



