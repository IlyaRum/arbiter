//package arbiter.data;
//
//import java.time.Instant;
//
//public class Result {
//  private String id;
//  private String name;
//  protected double value;
//  protected Instant time;
//
//  public Result(String name, String id) {
//    this.name = name;
//    this.id = id != null ? id.toLowerCase() : null;
//    this.value = 0;
//    this.time = Instant.now();
//  }
//
//  public boolean writable() {
//    return id != null && id.length() == 36;
//  }
//
//  public void setValue(double value, Instant time) {
//    this.value = value;
//    this.time = time;
////        if (writable() && unit.getCollection().isWriteEnable()) {
////            //unit.getCollection().addToWriteBuffer(new Measurement(id, ZonedDateTime.now(), value));
////        }
//  }
//
//  public int getInt() {
//    return (int) Math.round(value);
//  }
//
//  public String getId() {
//    return id;
//  }
//
//  public Instant getTime() {
//    return time;
//  }
//
//  public String getName() {
//    return name;
//  }
//
//  public double getValue() {
//    return value;
//  }
//
////    public Unit getUnit() {
////        return unit;
////    }
//}
