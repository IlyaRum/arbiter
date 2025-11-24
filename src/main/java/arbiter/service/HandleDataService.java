package arbiter.service;


import arbiter.constants.ParameterMappingConstants;
import arbiter.data.*;
import arbiter.di.DependencyInjector;
import arbiter.measurement.Measurement;
import arbiter.measurement.MeasurementList;
import com.fasterxml.jackson.core.JsonProcessingException;
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
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HandleDataService extends ABaseService{

  private boolean firstTime = true;
  private final ExecutorService executor = Executors.newSingleThreadExecutor();
  private static final EventFormat JSON_FORMAT = EventFormatProvider.getInstance().resolveFormat(JsonFormat.CONTENT_TYPE);
  private static final Logger logger = LoggerFactory.getLogger(HandleDataService.class);
  private final Map<String, MemoryData> store = new HashMap<>();
  private String currentChannelId;
  private final DependencyInjector dependencyInjector;

  //private final Set<String> targetUids;
  //private final Map<String, Measurement> dataBuffer;
  //private final Map<String, Instant> lastTimeStamps;
  //private boolean initialDataLoaded = false;
  //private final Map<String, Double> previousParameterValues = new ConcurrentHashMap<>();
  /**
   * Map для накопления изменений из разных событий
   */
  //private final Map<String, Parameter> accumulatedChanges = new ConcurrentHashMap<>();

  //ключ - ID юнита, значение - набор целевых UID для targetUids юнита
  private final Map<String, Set<String>> unitTargetUids = new ConcurrentHashMap<>();
  private final Map<String, Map<String, Measurement>> unitDataBuffers = new ConcurrentHashMap<>();
  private final Map<String, Map<String, Instant>> unitLastTimeStamps = new ConcurrentHashMap<>();
  private final Map<String, Boolean> unitInitialDataLoaded = new ConcurrentHashMap<>();
  private final Map<String, Map<String, Double>> unitPreviousParameterValues = new ConcurrentHashMap<>();
  private final Map<String, Map<String, Parameter>> unitAccumulatedChanges = new ConcurrentHashMap<>();


  public HandleDataService(Vertx vertx, DependencyInjector dependencyInjector) {
    super(vertx);
    this.dependencyInjector = dependencyInjector;
    //this.dataBuffer = new ConcurrentHashMap<>();
    //this.lastTimeStamps = new ConcurrentHashMap<>();

    initializeUnitTargetUids();

//    this.targetUids = Set.of(
//      "e3fab8a4-9985-4bd6-9066-ae7f70c12db3",
//      "5176d61e-09ab-4230-b92b-77a0d79f8380",
//      "fe2856ca-2e60-4660-9e31-825e733cc06b",
//      "42f5cb41-88c3-448d-911f-01ef39bc7586"
//    );
  }

  public Handler<String> handleTextMessage(Promise<JsonObject> promise) {
    return message -> {
      try {
        logger.debug("Input message: " + message);

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
    logger.debug("[data.v2]event: " + event);
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
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < dataArray.size(); i++) {
      JsonObject item = dataArray.getJsonObject(i);
      JsonNode jsonNode = item.mapTo(JsonNode.class);
      measurementList.add(jsonNode);
      String uid = item.getString("uid");
      double value = item.getDouble("value");

      if (i > 0) {
        result.append("; ");
      }
      result.append(String.format("%s = %f", uid, value));
    }

    onDataReceived(measurementList);
    //TODO[IER]
    //logAsync("Result: " + result);
  }

  private void handleRTEvents(CloudEvent event) {
    logger.debug("[rt-events]event: " + event);
    CloudEventData cloudEventData = event.getData();
    logAsync("[rt-events]CloudEventData: " + cloudEventData);
  }

  private void handleChannelOpened(CloudEvent event) {
    logger.debug("[channel.opened]event: " + event);
    logAsync("[channel.opened]event: " + event);
    currentChannelId = event.getSubject();
  }

  // Конвертация CloudEvent в строку JSON
  public static String cloudEventToString(CloudEvent event) {
    byte[] bytes = JSON_FORMAT.serialize(event);
    return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
  }

  //тут получаем данные из СК-11 и сохраняем их
  private void onDataReceived(MeasurementList list) {
    StoreData result = new StoreData();

    //TODO[IER]Для разработки. Удалить.
    if (firstTime) {
      logger.debug("measurementList = " + list);
    }

    try {
      for (int i = 0; i < list.size(); i++) {
        Measurement measurement = list.get(i);
        MemoryData memoryData = createMemoryData(measurement);

        //store.put(memoryData.getId(), memoryData);

        // Items[j].Parameters.Data[k]
        List<Unit> units = dependencyInjector.getUnitCollection().getUnits();
        for (Unit unit : units) {
          processParameters(memoryData, result, unit);
          processTopologies(memoryData, result, unit);
          processElements(memoryData, result, unit);
          processInfluencingFactors(memoryData, result, unit);
          processRepairSchema(memoryData, result, unit);
        }
      }

      if (result.size() > 0) {
        //dataProcessor.accept(result);
        dataBatchAggregator(list.getMeasurements(), result);

        if (firstTime) {
          logger.debug(String.format("### получено %d новых значений : %s", result.size(), result));
          String jsonData = convertStoreDataToJson(Collections.singletonList(result.getUnitDataList()));
          sendPostRequestAsync(jsonData);
          firstTime = false;
        }
      }

    } catch (Exception e) {
      logger.error("Ошибка при обработке данных измерений", e);
    }
  }

  /**
   * Агрегатор данных для каждого юнита отдельно
   */
  private void dataBatchAggregator(List<Measurement> measurements, StoreData result) {
    // Группируем измерения по UID для быстрого доступа
    Map<String, Measurement> measurementsByUid = new HashMap<>();
    for (Measurement measurement : measurements) {
      measurementsByUid.put(measurement.getUid().toLowerCase(), measurement);
    }

// Обрабатываем каждый юнит отдельно
    for (UnitDto unitDto : result.getUnitDataList()) {
      String unitId = getUnitIdentifier(unitDto);

      if (!unitTargetUids.containsKey(unitId)) {
        logger.debug("No target UIDs found for unit: " + unitId);
        continue;
      }

      Set<String> targetUids = unitTargetUids.get(unitId);
      Map<String, Measurement> dataBuffer = unitDataBuffers.get(unitId);
      Map<String, Instant> lastTimeStamps = unitLastTimeStamps.get(unitId);
      boolean initialDataLoaded = unitInitialDataLoaded.get(unitId);
      Map<String, Double> previousParameterValues = unitPreviousParameterValues.get(unitId);
      Map<String, Parameter> accumulatedChanges = unitAccumulatedChanges.get(unitId);

      boolean timeStampChanged = false;
      Set<String> receivedUids = new HashSet<>();

      // Проверяем получение целевых UID для этого юнита
      for (String targetUid : targetUids) {
        Measurement measurement = measurementsByUid.get(targetUid);
        if (measurement != null) {
          receivedUids.add(targetUid);
          timeStampChanged |= isTimestampChange(unitId, measurement);
        }
      }

      // Проверяем, все ли целевые UID получены для этого юнита
      boolean allTargetUidsReceived = receivedUids.containsAll(targetUids);

      // Если это первая загрузка и получены все UID
      if (!initialDataLoaded && allTargetUidsReceived) {
        unitInitialDataLoaded.put(unitId, true);
        logger.debug("Первоначальные данные загружены для юнита " + unitId + ": " + receivedUids);

        // Сохраняем первоначальные значения для этого юнита
        saveCurrentParameterValues(unitId, result);
      }

      boolean hasConsistentTimestamp = hasConsistentTimestamp(unitId);

      logger.debug(
        "Unit " + unitId +
          " hasConsistentTimestamp: " + hasConsistentTimestamp +
          " timeStampChanged : " + timeStampChanged +
          " initialDataLoaded " + initialDataLoaded +
          " dataBuffer: " + dataBuffer +
          " targetUids: " + targetUids +
          ", receivedUids: " + receivedUids
      );

      accumulateChanges(unitId, result);

      if (timeStampChanged
        && initialDataLoaded
        && dataBuffer.size() == targetUids.size()
        && hasConsistentTimestamp
        && !accumulatedChanges.isEmpty()
      ) {

        StoreData accumulatedResult = createStoreDataFromAccumulatedChanges(unitId);
        generateOutputJson(unitId);

        String jsonData = convertStoreDataToJson(Collections.singletonList(accumulatedResult.getUnitDataList()));
        logger.debug("Отправляем PUT запрос для юнита " + unitId + ": " + jsonData);

        saveCurrentParameterValuesFromAccumulated(unitId);

        // Отправляем запрос
        //sendPutRequestAsync(jsonData);

        accumulatedChanges.clear();
      }
    }
  }

  /**
   * Накапливает изменения для конкретного юнита
   */
  private void accumulateChanges(String unitId, StoreData result) {
    Map<String, Parameter> accumulatedChanges = unitAccumulatedChanges.get(unitId);
    // Находим UnitDto для этого юнита
    UnitDto unitDto = findUnitDtoById(result, unitId);
    if (unitDto == null) return;

    for (Parameter param : unitDto.getParameters().values()) {
      String paramId = param.getId();
      double currentValue = param.getValue();

      if (hasParameterValueChanged(unitId, paramId, currentValue)) {
        accumulatedChanges.put(paramId, param);
        logger.debug("Parameter changed for unit " + unitId + ": " +
          param.getName() + " = " + currentValue);
      }
    }
  }

  /**
   * Находит UnitDto по идентификатору юнита
   */
  private UnitDto findUnitDtoById(StoreData result, String unitId) {
    for (UnitDto unitDto : result.getUnitDataList()) {
      if (getUnitIdentifier(unitDto).equals(unitId)) {
        return unitDto;
      }
    }
    return null;
  }

  /**
   * Создает StoreData из накопленных изменений, когда все условия выполнены:
   * все targetUids получены, одинаковый timestamp, initialDataLoaded, hasConsistentTimestamp
   */
  private StoreData createStoreDataFromAccumulatedChanges(String unitId) {
    StoreData accumulatedResult = new StoreData();
    Map<String, Parameter> accumulatedChanges = unitAccumulatedChanges.get(unitId);

    if (!accumulatedChanges.isEmpty()) {
      Unit unit = findUnitByName(unitId);
      if (unit != null) {
        Map<String, Parameter> changesForUnit = new HashMap<>();

        for (Parameter param : accumulatedChanges.values()) {
          if (isParameterBelongsToUnit(unit, param)) {
            changesForUnit.put(getMappedParameterKey(param), param);
          }
        }

        if (!changesForUnit.isEmpty()) {
          UnitDto unitDto = new FilteredUnitDto(new UnitDto(unit), changesForUnit);
          accumulatedResult.addUnitData(unitDto);
          logger.debug("Created StoreData with " + changesForUnit.size() +
            " changed parameters for unit: " + unitId);
        }
      }
    }

    return accumulatedResult;
  }

  /**
   * Находит юнит по имени
   */
  private Unit findUnitByName(String unitName) {
    List<Unit> units = dependencyInjector.getUnitCollection().getUnits();
    for (Unit unit : units) {
      if (unit.getName().equals(unitName)) {
        return unit;
      }
    }
    return null;
  }

  /**
   * Проверяет принадлежность параметра юниту
   */
  private boolean isParameterBelongsToUnit(Unit unit, Parameter param) {
    for (Parameter unitParam : unit.getParameters()) {
      if (unitParam.getId().equals(param.getId())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Сохраняем текущие значения как предыдущие для следующего сравнения для конкретного юнита
   */
  private void saveCurrentParameterValuesFromAccumulated(String unitId) {
    Map<String, Parameter> accumulatedChanges = unitAccumulatedChanges.get(unitId);
    Map<String, Double> previousParameterValues = unitPreviousParameterValues.get(unitId);
    for (Parameter param : accumulatedChanges.values()) {
      previousParameterValues.put(param.getId(), param.getValue());
      logger.debug("Updated previous value for unit " + unitId +
        ": " + param.getName() + " = " + param.getValue());
    }
  }

  /**
   * Получает ключ для маппинга параметра (аналогично getMappedParameterKey в UnitDto)
   */
  private String getMappedParameterKey(Parameter param) {
    //TODO [IER] Используем ту же логику, что и в UnitDto
    // Нужно отрефакторить, чтобы использовать один метод
    return ParameterMappingConstants.PARAMETER_NAME_TO_FIELD_MAPPING.getOrDefault(param.getName(), param.getId());
  }

  /**
   * Находит юнит для параметра
   */
  private Unit findUnitForParameter(Parameter param) {
    List<Unit> units = dependencyInjector.getUnitCollection().getUnits();
    for (Unit unit : units) {
      for (Parameter unitParam : unit.getParameters()) {
        if (unitParam.getId().equals(param.getId())) {
          return unit;
        }
      }
    }
    return null;
  }


  /**
   * Сохраняет текущие значения параметров для конкретного юнита
   */
  private void saveCurrentParameterValues(String unitId, StoreData result) {
    Map<String, Double> previousParameterValues = unitPreviousParameterValues.get(unitId);
    UnitDto unitDto = findUnitDtoById(result, unitId);
    if (unitDto != null) {
        for (Parameter param : unitDto.getParameters().values()) {
          String paramId = param.getId();
          double currentValue = param.getValue();
          previousParameterValues.put(paramId, currentValue);
          logger.debug("Saved initial value for unit " + unitId +
            ": " + param.getName() + " = " + currentValue);
        }
    }
  }

  /**
   * Проверяет, изменилось ли значение параметра.
   * Сравнивает текущее значение с предыдущим сохраненным значением
   */
  private boolean hasParameterValueChanged(String unitId, String paramId, double currentValue) {
    Map<String, Double> previousParameterValues = unitPreviousParameterValues.get(unitId);
    Double previousValue = previousParameterValues.get(paramId);
    // Если предыдущего значения нет - считаем что изменилось (первый раз)
    if (previousValue == null) {
      return true;
    }

    // Сравниваем с учетом точности double
    return Math.abs(currentValue - previousValue) > 1e-10;
  }


  private boolean isTimestampChange(String unitId, Measurement measurement) {
    String uid = measurement.getUid().toLowerCase();
    Instant  currentTimeStamp = Instant.parse(measurement.getTimeStamp());


    Map<String, Instant> lastTimeStamps = unitLastTimeStamps.get(unitId);
    Map<String, Measurement> dataBuffer = unitDataBuffers.get(unitId);

    Instant lastTimeStamp = lastTimeStamps.get(uid);
    lastTimeStamps.put(uid, currentTimeStamp);

    dataBuffer.put(uid, measurement);

    // Возвращаем true если timestamp изменился
    return lastTimeStamp != null && !lastTimeStamp.equals(currentTimeStamp);
  }


  /**
   * Проверяет согласованность timestamp для конкретного юнита
   *
   * True - если все timestamps имеют одинаковую метку времени
   * False - если хотя бы один timestamps отличается от других
   */
  private boolean hasConsistentTimestamp(String unitId) {
    Map<String, Measurement> dataBuffer = unitDataBuffers.get(unitId);
    if (dataBuffer.isEmpty()) return false;

    Instant firstTimestamp = null;
    for (Measurement item : dataBuffer.values()) {
      if (firstTimestamp == null) {
        firstTimestamp = Instant.parse(item.getTimeStamp());
      } else if (!firstTimestamp.equals(Instant.parse(item.getTimeStamp()))) {
        return false;
      }
    }
    return true;
  }

  private void generateOutputJson(String unitId) {
    Map<String, Measurement> dataBuffer = unitDataBuffers.get(unitId);
    List<Measurement> allData = new ArrayList<>(dataBuffer.values());
    allData.sort(Comparator.comparing(Measurement::getUid));
    String resultJson = convertStoreDataToJson(Collections.singletonList(allData));
    logger.debug("Выходной JSON с одинаковым timestamp :" + resultJson);
  }

  private void sendPostRequestAsync(String jsonData) {
    executor.submit(() -> {
      try {
        sendPostRequest(jsonData);
      } catch (Exception e) {
        logger.error("Ошибка при асинхронной отправке данных", e);
      }
    });
  }

  private MemoryData createMemoryData(Measurement measurement) {
    String id = measurement.getUid();
    double value = measurement.getValue();
    Instant time = Instant.parse(measurement.getTimeStamp());
    int qCode = measurement.getQCode();

    return new MemoryData(id, value, time, qCode);
  }

  private void processParameters(MemoryData memoryData, StoreData result, Unit unit) {
    logger.debug("-------");
    List<Parameter> parameters = unit.getParameters();
    UnitDto unitDto;

    // Аналог: for k := 0 to Items[j].Parameters.Count - 1 do
    for (Parameter parameter : parameters) {

      // Аналог: if CompareText(P.Id, Data.Id) = 0 then
      if (parameter.getId().equalsIgnoreCase(memoryData.getId())) {

        // Проверяем, изменились ли данные
        // Аналог: if not P.Assigned or (P.Time <> Data.Time) or (P.Value <> Data.Value) then
        boolean isDataDifferent = parameter.isDataDifferent(memoryData.getValue(), memoryData.getTime());
        logger.debug("parameterId=" + parameter.getId() +
          " / parameterName=" + parameter.getName() +
          " / parameterValue=" + parameter.getValue() +

          " /----/ memoryData=" + memoryData + " / isDataDifferent=" + isDataDifferent);
        if (isDataDifferent) {

          // Аналог: P.SetData(Data.Value, Data.Time, Data.QCode)
          parameter.setData(memoryData.getValue(), memoryData.getTime(), memoryData.getQCode());

          // Создаем или получаем UnitData для текущего юнита
          unitDto = result.getUnitData(unit);
          if (unitDto == null) {
            unitDto = new UnitDto(unit);
            result.addUnitData(unitDto);
          }

          //unitDto.addParameter(parameter);
        }
//          logger.debug(String.format("%s: %s/%s= %f [%s] %s",
//            unit.getName(), parameter.getId(), parameter.getName(), memoryData.getValue(),
//            Integer.toHexString(memoryData.getQCode()),
//            memoryData.getTime().toString()));
        return;
      }
    }
  }

  private void processTopologies(MemoryData memoryData, StoreData result, Unit unit) {

    List<Topology> topologyList = unit.getTopologies();
    UnitDto unitDto;

    for (Topology topology : topologyList) {

      if (topology.getId().equalsIgnoreCase(memoryData.getId())) {

        if (topology.isDataDifferent(memoryData.getValue(), memoryData.getTime())) {

          topology.setData(memoryData.getValue(), memoryData.getTime(), memoryData.getQCode());

          unitDto = result.getUnitData(unit);
          if (unitDto == null) {
            unitDto = new UnitDto(unit);
            result.addUnitData(unitDto);
          }
        }
        return;
      }
    }
  }

  private void processElements(MemoryData memoryData, StoreData result, Unit unit) {
    List<Element> elements = unit.getElements();
    UnitDto unitDto;

    for (Element element : elements) {

      if (element.getId().equalsIgnoreCase(memoryData.getId())) {

        if (element.isDataDifferent(memoryData.getValue(), memoryData.getTime())) {

          element.setData(memoryData.getValue(), memoryData.getTime(), memoryData.getQCode());

          unitDto = result.getUnitData(unit);
          if (unitDto == null) {
            unitDto = new UnitDto(unit);
            result.addUnitData(unitDto);
          }
        }
        return;
      }
    }
  }

  private void processRepairSchema(MemoryData memoryData, StoreData result, Unit unit) {
    Optional.ofNullable(unit.getRepairSchema())
      .map(RepairSchema::getRepairGroupValues)
      .orElse(Collections.emptyList())
      .forEach(repairGroupValue -> processRepairGroupValue(memoryData, result, unit, repairGroupValue));
  }

  private void processRepairGroupValue(MemoryData memoryData, StoreData result, Unit unit, RepairGroupValue repairGroupValue) {
    List<Composition> compositions = repairGroupValue.getValues();
    for (Composition composition : compositions) {
      if (composition.getId().equalsIgnoreCase(memoryData.getId())) {
        if (composition.isDataDifferent(memoryData.getValue(), memoryData.getTime())) {
          composition.setData(memoryData.getValue(), memoryData.getTime(), memoryData.getQCode());
          UnitDto unitDto = result.getUnitData(unit);
          if (unitDto == null) {
            unitDto = new UnitDto(unit);
            result.addUnitData(unitDto);
          }
        }
        return;
      }
    }
  }

  private void processInfluencingFactors(MemoryData memoryData, StoreData result, Unit unit) {
    List<InfluencingFactor> influencingFactors = unit.getInfluencingFactors();
    UnitDto unitDto;

    for (InfluencingFactor influencingFactor : influencingFactors) {

      if (influencingFactor.getId().equalsIgnoreCase(memoryData.getId())) {

        if (influencingFactor.isDataDifferent(memoryData.getValue(), memoryData.getTime())) {

          influencingFactor.setData(memoryData.getValue(), memoryData.getTime(), memoryData.getQCode());

          unitDto = result.getUnitData(unit);
          if (unitDto == null) {
            unitDto = new UnitDto(unit);
            result.addUnitData(unitDto);
          }
        }
        return;
      }
    }
  }

  private void sendPostRequest(String jsonData) {
    WebClient client = WebClient.create(vertx);

    logger.debug("Отправляем POST запрос в арбитр расчетов: " + jsonData);

    client.postAbs("https://your-api-endpoint.com/data")
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

  private String convertStoreDataToJson(List<Object> objects) {
    try {
      ObjectMapper mapper = new ObjectMapper();
      mapper.registerModule(JsonFormat.getCloudEventJacksonModule());
      mapper.registerModule(new JavaTimeModule());
      mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
      mapper.enable(SerializationFeature.INDENT_OUTPUT);

//      CloudEvent cloudEvent = CloudEventBuilder.v1()
//        .withId(UUID.randomUUID().toString())
//        .withSource(URI.create("urn:store:data"))
//        .withType("StoreDataEvent")
//        .withData(mapper.writeValueAsBytes(result))
//        .build();
      return mapper.writeValueAsString(objects);

    } catch (Exception e) {
      logger.error(e.getMessage());
      return e.getMessage();
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

  /**
   * Класс для UnitDto с фильтрованными параметрами
   */
  private static class FilteredUnitDto extends UnitDto {
    private final Map<String, Parameter> filteredParameters;

    public FilteredUnitDto(UnitDto original, Map<String, Parameter> filteredParameters) {
      super(original.getUnit());
      this.filteredParameters = filteredParameters;
    }

    @Override
    public Map<String, Parameter> getParameters() {
      return filteredParameters;
    }
  }

  /**
   * Инициализирует целевые UID для каждого юнита из конфигурации JSON
   */
  private void initializeUnitTargetUids() {
    List<Unit> units = dependencyInjector.getUnitCollection().getUnits();
    logger.debug("Initialized units: " + units);
    for (Unit unit : units) {
      Set<String> targetUids = extractTargetUidsFromUnit(unit);
      unitTargetUids.put(getUnitIdentifier(unit), targetUids);

      // Инициализируем структуры данных для этого юнита
      unitDataBuffers.put(getUnitIdentifier(unit), new ConcurrentHashMap<>());
      unitLastTimeStamps.put(getUnitIdentifier(unit), new ConcurrentHashMap<>());
      unitInitialDataLoaded.put(getUnitIdentifier(unit), false);
      unitPreviousParameterValues.put(getUnitIdentifier(unit), new ConcurrentHashMap<>());
      unitAccumulatedChanges.put(getUnitIdentifier(unit), new ConcurrentHashMap<>());

      logger.debug("Initialized target UIDs for unit " + unit.getName() + ": " + targetUids);
    }
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
        logger.debug("Added target parameter for unit " + unit.getName() +
          ": " + paramName + " (ID: " + param.getId() + ")");
      }
    }

    return targetUids;
  }

  /**
   * Получает идентификатор юнита (используем имя как идентификатор)
   */
  private String getUnitIdentifier(Unit unit) {
    return unit.getName();
  }

  /**
   * Получает идентификатор юнита из UnitDto
   */
  private String getUnitIdentifier(UnitDto unitDto) {
    return unitDto.getName();
  }

  /**
   * Проверяет, является ли параметр целевым для отслеживания
   */
  private boolean isTargetParameter(String parameterName) {
    return parameterName.contains("МДП без ПА") ||
      parameterName.contains("МДП с ПА") ||
      parameterName.contains("АДП") ||
      parameterName.contains("Номер цикла расчета");
  }
}
