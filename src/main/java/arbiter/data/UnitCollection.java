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
        this.instance = config.getString(CONFIG_KEY_INSTANCE);

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

  public List<Unit> getUnits() {
    return units;
  }

  public String getEventUID() {
    return eventUID;
  }

  /**
   * Определяет, нужно ли отслеживать события изменения критерия МДП СМЗУ в системе.
   */
  public boolean isCheckEvent() {
    return checkEvent;
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
    commonField.setInstance(instance);
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

}



