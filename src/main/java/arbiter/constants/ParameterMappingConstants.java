package arbiter.constants;

import java.util.Map;

public class ParameterMappingConstants {

  public static final Map<String, String> PARAMETER_NAME_TO_FIELD_MAPPING = createMapping();

  private static Map<String, String> createMapping() {
    return Map.ofEntries(
      Map.entry("МДП ПУР", "maxAllowedOverflowByRules"),
      Map.entry("АДП ПУР", "failureAllowedOverflowByRules"),
      Map.entry("МДП без ПА [СМЗУ]", "maxAllowedOverflowNoFailureSMZU"),
      Map.entry("МДП с ПА [СМЗУ]", "maxAllowedOverflowWithFailureSMZU"),
      Map.entry("АДП [СМЗУ]", "failureAllowedOverflowSMZU"),
      Map.entry("Номер цикла расчета СМЗУ", "cycleNumberSMZU"),
      Map.entry("Состояние сечения", "sectionState"),
      Map.entry("УВК АДВ ПС", "automaticDosageExposureSubstationControlComplex"),
      Map.entry("dМДПпред/след_доп", "maxAllowOverflowDeltaPercents"),
      Map.entry("НК", "nonlinearOscillations"),
      Map.entry("Tрасч", "nextCycleTimeoutSMZU"),
      Map.entry("Nц", "maxAllowedCyclesWithSameOverflowValue"),
      Map.entry("Kручн", "handleControl"),
      Map.entry("Фактический переток (КПОС)", "realOverflowFromScada"),
      Map.entry("Количество циклов активации КС (норма)", "normalCyclesToBackSMZU"),
      Map.entry("X - время активности перехода на КПОС", "alarmDelayAfterSwitchingToRules"),
      Map.entry("Y - кол-во переходов на КПОС за время Z", "swithingNumberBetweenSMZUAndRules"),
      Map.entry("Z - фикс. время контроля перехода на КПОС", "comtrolTimeForSwithingBetweenSMZUAndRules"),
      Map.entry("max T ускор. р. цикла", "waitingTimeForAcceleratedCycleSMZU"),
      Map.entry("T блокировки в АОП (АРП)", "blockingTimeForOverflowRestrictionAutomatics")
    );
  }
}
