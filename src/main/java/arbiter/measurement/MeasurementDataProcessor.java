package arbiter.measurement;

import arbiter.data.*;
import arbiter.data.dto.CommonFieldDto;
import arbiter.data.dto.UnitDto;
import arbiter.data.model.*;
import arbiter.di.DependencyInjector;
import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutorService;

/**
 * Отвечает за базовую обработку измерений
 */

public class MeasurementDataProcessor {

  private static final Logger logger = LoggerFactory.getLogger(MeasurementDataProcessor.class);

  private boolean firstTime = true;
  private DataReadyCallback dataReadyCallback;
  private final DependencyInjector dependencyInjector;
  private MeasurementChangeTracker measurementChangeTracker;

  private final ExecutorService singleThreadExecutor;

  public MeasurementDataProcessor(DependencyInjector dependencyInjector, ExecutorService executor) {
    this.dependencyInjector = dependencyInjector;
    this.singleThreadExecutor = executor;
  }

  /**
   * Основной метод обработки полученных данных измерений
   */
  public void onDataReceived(MeasurementList list) {
    try {
      if (measurementChangeTracker == null && dataReadyCallback != null) {
        measurementChangeTracker = new MeasurementChangeTracker(dependencyInjector, dataReadyCallback, singleThreadExecutor);
      }

      if (measurementChangeTracker != null) {
        measurementChangeTracker.processAndTrackChanges(list);

        if (firstTime && dataReadyCallback != null) {
          firstTime = false;
          StoreData initialData = getAllCurrentData();
          if (initialData != null && initialData.size() > 0) {
            logger.info("Данные всех измерений подготовлены в StoreData. Размер: " + initialData.size());
            singleThreadExecutor.submit(() -> {
              try {
                dataReadyCallback.onDataReady(initialData, null);
              } catch (Exception e) {
                logger.error("Ошибка при вызове callback для отправки первоначальных данных", e);
              }
            });
          }
        }
      }
    } catch (Exception e) {
      logger.error("Ошибка при обработке данных измерений", e);
    }
  }

  /**
   * Получает все текущие данные для первоначальной отправки
   */
  private StoreData getAllCurrentData() {
    StoreData result = new StoreData();

    List<Unit> units = dependencyInjector.getUnitCollection().getUnits();
    for (Unit unit : units) {
      UnitDto unitDto = new UnitDto(unit);
      result.addUnitData(unitDto);
    }

    CommonFieldDto commonFieldDto = dependencyInjector.getUnitCollection().getCommonFieldDto();
    result.setCommonFieldDto(commonFieldDto);

    return result;
  }

  /**
   * Основной метод обработки полученных данных измерений
   */
//  public void onDataReceived1(MeasurementList list) {
//    try {
//      StoreData result = processMeasurementsToStoreData(list);
//
//      CommonFieldDto commonFieldDto = dependencyInjector.getUnitCollection().getCommonFieldDto();
//      result.setCommonFieldDto(commonFieldDto);
//
//      if (result.size() > 0) {
//        if (firstTime && dataReadyCallback != null) {
//          logger.info("Данные всех измерений подготовлены в StoreData. Размер: " + result.size());
//          firstTime = false;
//          singleThreadExecutor.submit(() -> {
//            try {
//              dataReadyCallback.onDataReady(result, null);
//            } catch (Exception e) {
//              logger.error("Ошибка при вызове callback для отправки первоначальных данных", e);
//            }
//          });
//        }
//
//        if (measurementChangeTracker == null && dataReadyCallback != null) {
//          measurementChangeTracker = new MeasurementChangeTracker(dependencyInjector, dataReadyCallback, singleThreadExecutor);
//        }
//
//        if (measurementChangeTracker != null) {
//          measurementChangeTracker.trackAndProcessChanges(list.getMeasurements(), result);
//        }
//      }
//
//    } catch (Exception e) {
//      logger.error("Ошибка при обработке данных измерений", e);
//    }
//  }

  /**
   * Преобразует список измерений в StoreData
   */
//  private StoreData processMeasurementsToStoreData(MeasurementList list) {
//    StoreData result = new StoreData();
//
//    logger.debug("measurementList = " + list);
//
//    for (int i = 0; i < list.size(); i++) {
//      Measurement measurement = list.get(i);
//      MemoryData memoryData = createMemoryData(measurement);
//
//      // Items[j].Parameters.Data[k]
//      List<Unit> units = dependencyInjector.getUnitCollection().getUnits();
//      for (Unit unit : units) {
//        processParameters(memoryData, result, unit);
//        processTopologies(memoryData, result, unit);
//        processElements(memoryData, result, unit);
//        processInfluencingFactors(memoryData, result, unit);
//        processRepairSchema(memoryData, result, unit);
//      }
//    }
//
//    return result;
//  }

  /**
   * Устанавливает callback для уведомления о готовых данных
   */
  public void setDataReadyCallback(DataReadyCallback callback) {
    this.dataReadyCallback = callback;
  }

//  private MemoryData createMemoryData(Measurement measurement) {
//    String id = measurement.getUid();
//    double value = measurement.getValue();
//    Instant time = Instant.parse(measurement.getTimeStamp());
//    int qCode = measurement.getQCode();
//
//    return new MemoryData(id, value, time, qCode);
//  }

//  private void processParameters(MemoryData memoryData, StoreData result, Unit unit) {
//    List<Parameter> parameters = unit.getParameters();
//
//    // Аналог: for k := 0 to Items[j].Parameters.Count - 1 do
//    for (Parameter parameter : parameters) {
//
//      // Аналог: if CompareText(P.Id, Data.Id) = 0 then
//      if (parameter.getId().equalsIgnoreCase(memoryData.getId())) {
//
//        // Проверяем, изменились ли данные
//        // Аналог: if not P.Assigned or (P.Time <> Data.Time) or (P.Value <> Data.Value) then
//        boolean isDataDifferent = parameter.isDataDifferent(memoryData.getValue(), memoryData.getTime());
//        if (isDataDifferent) {
//
//          // Аналог: P.SetData(Data.Value, Data.Time, Data.QCode)
//          parameter.setData(memoryData.getValue(), memoryData.getTime(), memoryData.getQCode());
//
//          // Создаем или получаем UnitData для текущего юнита
//          UnitDto unitDto = result.getUnitData(unit);
//          if (unitDto == null) {
//            unitDto = new UnitDto(unit);
//            result.addUnitData(unitDto);
//          }
//        }
//        return;
//      }
//    }
//  }
//
//  private void processTopologies(MemoryData memoryData, StoreData result, Unit unit) {
//    List<Topology> topologyList = unit.getTopologies();
//
//    for (Topology topology : topologyList) {
//      if (topology.getId().equalsIgnoreCase(memoryData.getId())) {
//        if (topology.isDataDifferent(memoryData.getValue(), memoryData.getTime())) {
//          topology.setData(memoryData.getValue(), memoryData.getTime(), memoryData.getQCode());
//          UnitDto unitDto = result.getUnitData(unit);
//          if (unitDto == null) {
//            unitDto = new UnitDto(unit);
//            result.addUnitData(unitDto);
//          }
//        }
//        return;
//      }
//    }
//  }
//
//  private void processElements(MemoryData memoryData, StoreData result, Unit unit) {
//    List<Element> elements = unit.getElements();
//
//    for (Element element : elements) {
//      if (element.getId().equalsIgnoreCase(memoryData.getId())) {
//        if (element.isDataDifferent(memoryData.getValue(), memoryData.getTime())) {
//          element.setData(memoryData.getValue(), memoryData.getTime(), memoryData.getQCode());
//          UnitDto unitDto = result.getUnitData(unit);
//          if (unitDto == null) {
//            unitDto = new UnitDto(unit);
//            result.addUnitData(unitDto);
//          }
//        }
//        return;
//      }
//    }
//  }
//
//  private void processRepairSchema(MemoryData memoryData, StoreData result, Unit unit) {
//    Optional.ofNullable(unit.getRepairSchema())
//      .map(RepairSchema::getRepairGroupValues)
//      .orElse(Collections.emptyList())
//      .forEach(repairGroupValue -> processRepairGroupValue(memoryData, result, unit, repairGroupValue));
//  }

//  private void processRepairGroupValue(MemoryData memoryData, StoreData result, Unit unit, RepairGroupValue repairGroupValue) {
//    List<Composition> compositions = repairGroupValue.getValues();
//    for (Composition composition : compositions) {
//      if (composition.getId().equalsIgnoreCase(memoryData.getId())) {
//        if (composition.isDataDifferent(memoryData.getValue(), memoryData.getTime())) {
//          composition.setData(memoryData.getValue(), memoryData.getTime(), memoryData.getQCode());
//          UnitDto unitDto = result.getUnitData(unit);
//          if (unitDto == null) {
//            unitDto = new UnitDto(unit);
//            result.addUnitData(unitDto);
//          }
//        }
//        return;
//      }
//    }
//  }

//  private void processInfluencingFactors(MemoryData memoryData, StoreData result, Unit unit) {
//    List<InfluencingFactor> influencingFactors = unit.getInfluencingFactors();
//
//    for (InfluencingFactor influencingFactor : influencingFactors) {
//      if (influencingFactor.getId().equalsIgnoreCase(memoryData.getId())) {
//        if (influencingFactor.isDataDifferent(memoryData.getValue(), memoryData.getTime())) {
//          influencingFactor.setData(memoryData.getValue(), memoryData.getTime(), memoryData.getQCode());
//          UnitDto unitDto = result.getUnitData(unit);
//          if (unitDto == null) {
//            unitDto = new UnitDto(unit);
//            result.addUnitData(unitDto);
//          }
//        }
//        return;
//      }
//    }
//  }
}
