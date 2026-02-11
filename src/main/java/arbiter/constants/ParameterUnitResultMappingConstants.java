package arbiter.constants;

import java.util.Map;

public class ParameterUnitResultMappingConstants {

  public static final Map<String, String> PARAMETER_RESULT_NAME_TO_FIELD_MAPPING = createMapping();

  private static Map<String, String> createMapping() {
    return Map.ofEntries(
      Map.entry("МДП без ПА (без НК) КПОС", "scadaMaxAllowedOverflowWithoutNO"),
      Map.entry("МДП с ПА (без НК) КПОС", "scadaMaxAllowedOverflowWithoutNOWithAntiAccident"),
      Map.entry("АДП (без НК) КПОС", "failureAllowedOverflowWithoutNonlinearOscillation"),
      Map.entry("Признак контроля КС в КПОС от (СМЗУ)", "attributeControlSection"),
      Map.entry("Переход на КПОС активен в течении X мин", "scadaDangerousSectionOverflowControlActiveTime"),
      Map.entry("Переход СМЗУ-КПОС Y раз в Z мин", "reserveMotitoringToScadaSwitchNumberForTimePeriod"),
      Map.entry("dМДПпред/след СМЗУ (изменение)", "prevNextMaxAllowedOverflowChangeFlag"),
      Map.entry("dМДПпред/след СМЗУ (разница)", "prevNextMonitoredMaxAllowedOverflowDelta"),
      Map.entry("dМДПпред/след СМЗУ (разница без %)", "prevNextMonitoredMaxAllowedOverflowDeltaPercent"),
      Map.entry("ДП СМЗУ/КПОС (разница без %)", "allowedOverflowDeltaMonitoredAndScadaValue"),
      Map.entry("ДП СМЗУ/КПОС (разница)", "allowedOverflowDeltaMonitoredAndScadaPercent"),
      Map.entry("Несовпадение знаков факт. перетока и КС", "factOverflowSignMismatch"),
      Map.entry("Изменение топологии сети", "topologyChanged"),
      Map.entry("Учет ШС в расчетной схеме", "useShuntConnections"),
      Map.entry("Отсутствие изменения № цикла расчета", "changeCycleNumberAbsence"),
      Map.entry("Отсутствие изменения результатов расчета", "changeCaclResultAbsence"),
      Map.entry("Короткий цикл СМЗУ", "shortCycle"),
      Map.entry("Критерий \"запись\"", "writeCryterion"),
      Map.entry("Нет записи ДП СМЗУ в КПОС", "noRecordAllowedOverflowInScada"),
      Map.entry("блокировка АОП КПОС/СМЗУ", "blockAutomaticOffOverflow"),
      Map.entry("T до снятия блокировки в АОП (АРП)", "timeBeforeBlockOff"),
      Map.entry("UID блокировки АОП зависимых сечений", "blockAutomaticOverflowItems"),
      Map.entry("T до снятия блокировки", "blockAutomaticOverflowTimeBeforeOffItems")
    );
  }
}
