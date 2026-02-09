package arbiter.constants;

import java.util.Map;

public class ParameterArpmMappingConstants {

  public static final Map<String, String> PARAMETER_ARPM_NAME_TO_FIELD_MAPPING = createMapping();

  private static Map<String, String> createMapping() {
    return Map.ofEntries(
      Map.entry("Арбитр. Не пройдена достоверизация уставки", "verificationFalied"),
      Map.entry("АРПМ адаптивная уставка чтение", "adaptiveReadSetpoint"),
      Map.entry("АРПМ адаптивная уставка запись", "adaptiveWriteSetpoint"),
      Map.entry("АРПМ дельта", "delta"),
      Map.entry("АРПМ Тз", "wtiteTimeout"),
      Map.entry("АРПМ превышение записанного", "overflowRecorded"),
      Map.entry("АРПМ превышение предыдущего", "overflowPrevious"),
      Map.entry("состояние АРПМ", "state")
    );
  }
}
