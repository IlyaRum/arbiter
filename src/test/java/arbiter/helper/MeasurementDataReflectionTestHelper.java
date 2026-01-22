package arbiter.helper;

public class MeasurementDataReflectionTestHelper {

  public MeasurementDataReflectionTestHelper() {
  }

  public void setField(Object object, String fieldName, Object value) {
    try {
      java.lang.reflect.Field field = object.getClass().getDeclaredField(fieldName);
      field.setAccessible(true);
      field.set(object, value);
    } catch (Exception e) {
      throw new RuntimeException("Failed to set field: " + fieldName, e);
    }
  }
}
