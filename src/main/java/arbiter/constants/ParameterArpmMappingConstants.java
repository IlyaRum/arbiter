package arbiter.constants;

import java.util.Map;

import static arbiter.constants.UnitCollectionConstants.*;

public class ParameterArpmMappingConstants {

  public static final Map<String, String> PARAMETER_ARPM_NAME_TO_FIELD_MAPPING = createMapping();

  private static Map<String, String> createMapping() {
    return Map.ofEntries(
      Map.entry(ARPM_PARAM_ARBITR_NOT_VALID, "verificationFalied"),
      Map.entry(ARPM_PARAM_ADAPTIVE_SETPOINT_READ, "adaptiveReadSetpoint"),
      Map.entry(ARPM_PARAM_ADAPTIVE_SETPOINT_WRITE, "adaptiveWriteSetpoint"),
      Map.entry(ARPM_PARAM_DELTA, "delta"),
      Map.entry(ARPM_PARAM_TZ, "wtiteTimeout"),
      Map.entry(ARPM_PARAM_EXCEED_WRITTEN, "overflowRecorded"),
      Map.entry(ARPM_PARAM_EXCEED_PREVIOUS, "overflowPrevious"),
      Map.entry(ARPM_PARAM_STATE, "state")
    );
  }
}
