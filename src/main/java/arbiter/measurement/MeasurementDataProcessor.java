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

        if (firstTime && dataReadyCallback != null) {
          StoreData initialData = getAllCurrentData();
          if (initialData != null && initialData.size() > 0) {
            logger.info("Срез всех измерений подготовлен для POST запроса. Размер: " + initialData.size());
            singleThreadExecutor.submit(() -> {
              try {
                dataReadyCallback.onDataReady(initialData, null);
              } catch (Exception e) {
                logger.error("Ошибка при вызове callback для отправки первоначальных данных", e);
              }
            });
          }
          firstTime = false;
          logger.debug("Флаг firstTime сброшен после обработки первого среза измерений");
        }
        if (measurementChangeTracker != null) {
          measurementChangeTracker.processAndTrackChanges(list);
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

    CommonField commonField = dependencyInjector.getUnitCollection().getCommonField();
    CommonFieldDto commonFieldDto = new CommonFieldDto(commonField);
    result.setCommonFieldDto(commonFieldDto);

    return result;
  }

  public void resetFirstTime() {
    this.firstTime = true;
    logger.info("Флаг firstTime выставлен в true");
  }

  /**
   * Устанавливает callback для уведомления о готовых данных
   */
  public void setDataReadyCallback(DataReadyCallback callback) {
    this.dataReadyCallback = callback;
  }

  public boolean isFirstTime() {
    return firstTime;
  }

  /**
   * Полный сброс состояния процессора и трекера изменений
   */
  public void reset() {
    if (measurementChangeTracker != null) {
      measurementChangeTracker.reset();
    }
    resetFirstTime();
  }
}
