package arbiter.measurement;

import arbiter.data.*;
import arbiter.data.dto.UnitDto;
import arbiter.data.model.*;
import arbiter.di.DependencyInjector;
import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Отвечает за базовую обработку измерений
 */

public class MeasurementDataProcessor {

  private static final Logger logger = LoggerFactory.getLogger(MeasurementDataProcessor.class);

  private boolean firstTime = true;
  private DataReadyCallback dataReadyCallback;
  private final DependencyInjector dependencyInjector;
  private BatchAggregator batchAggregator;

  private final ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor(r -> {
    Thread t = new Thread(r, "Callback-Processor");
    t.setDaemon(true);
    return t;
  });


  public MeasurementDataProcessor(DependencyInjector dependencyInjector) {
    this.dependencyInjector = dependencyInjector;
  }

  /**
   * Основной метод обработки полученных данных измерений
   */
  public void onDataReceived(MeasurementList list) {
    logger.debug("Processing measurement list with size: " + list.size());

    try {
      StoreData result = processMeasurementsToStoreData(list);

      if (firstTime && dataReadyCallback != null) {
        logger.info("Первичная отправка всех изменений в расчетный сервис. Размер: " + result.size());
        firstTime = false;
        singleThreadExecutor.submit(() -> {
          try {
            dataReadyCallback.onDataReady(result, null);
          } catch (Exception e) {
            logger.error("Ошибка при обработке первоначальных данных", e);
          }
        });
      }

      if (result.size() > 0) {
        if (batchAggregator == null && dataReadyCallback != null) {
          batchAggregator = new BatchAggregator(dependencyInjector, dataReadyCallback, singleThreadExecutor);
        }

        if (batchAggregator != null) {
          batchAggregator.aggregateData(list.getMeasurements(), result);
        }
      }

    } catch (Exception e) {
      logger.error("Ошибка при обработке данных измерений", e);
    }
  }

  /**
   * Преобразует список измерений в StoreData
   */
  private StoreData processMeasurementsToStoreData(MeasurementList list) {
    StoreData result = new StoreData();

    logger.debug("measurementList = " + list);

    for (int i = 0; i < list.size(); i++) {
      Measurement measurement = list.get(i);
      MemoryData memoryData = createMemoryData(measurement);

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

    return result;
  }

  /**
   * Устанавливает callback для уведомления о готовых данных
   */
  public void setDataReadyCallback(DataReadyCallback callback) {
    this.dataReadyCallback = callback;
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

  private MemoryData createMemoryData(Measurement measurement) {
    String id = measurement.getUid();
    double value = measurement.getValue();
    Instant time = Instant.parse(measurement.getTimeStamp());
    int qCode = measurement.getQCode();

    return new MemoryData(id, value, time, qCode);
  }

  private void processParameters(MemoryData memoryData, StoreData result, Unit unit) {
    List<Parameter> parameters = unit.getParameters();

    // Аналог: for k := 0 to Items[j].Parameters.Count - 1 do
    for (Parameter parameter : parameters) {

      // Аналог: if CompareText(P.Id, Data.Id) = 0 then
      if (parameter.getId().equalsIgnoreCase(memoryData.getId())) {

        // Проверяем, изменились ли данные
        // Аналог: if not P.Assigned or (P.Time <> Data.Time) or (P.Value <> Data.Value) then
        boolean isDataDifferent = parameter.isDataDifferent(memoryData.getValue(), memoryData.getTime());
        if (isDataDifferent) {

          // Аналог: P.SetData(Data.Value, Data.Time, Data.QCode)
          parameter.setData(memoryData.getValue(), memoryData.getTime(), memoryData.getQCode());

          // Создаем или получаем UnitData для текущего юнита
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

  private void processTopologies(MemoryData memoryData, StoreData result, Unit unit) {
    List<Topology> topologyList = unit.getTopologies();

    for (Topology topology : topologyList) {
      if (topology.getId().equalsIgnoreCase(memoryData.getId())) {
        if (topology.isDataDifferent(memoryData.getValue(), memoryData.getTime())) {
          topology.setData(memoryData.getValue(), memoryData.getTime(), memoryData.getQCode());
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

  private void processElements(MemoryData memoryData, StoreData result, Unit unit) {
    List<Element> elements = unit.getElements();

    for (Element element : elements) {
      if (element.getId().equalsIgnoreCase(memoryData.getId())) {
        if (element.isDataDifferent(memoryData.getValue(), memoryData.getTime())) {
          element.setData(memoryData.getValue(), memoryData.getTime(), memoryData.getQCode());
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

    for (InfluencingFactor influencingFactor : influencingFactors) {
      if (influencingFactor.getId().equalsIgnoreCase(memoryData.getId())) {
        if (influencingFactor.isDataDifferent(memoryData.getValue(), memoryData.getTime())) {
          influencingFactor.setData(memoryData.getValue(), memoryData.getTime(), memoryData.getQCode());
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
}
