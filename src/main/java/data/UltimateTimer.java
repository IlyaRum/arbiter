package data;

import java.time.Duration;
import java.time.Instant;

public class UltimateTimer {
  private final String unitName;
  private final String type;
  private Instant startTime;
  private long value;
  private boolean running;

  public UltimateTimer(String unitName, String type) {
    this.unitName = unitName;
    this.type = type;
  }

  public void start(long durationSeconds) {
    this.startTime = Instant.now();
    this.running = true;
    this.value = 1;
  }

  public void stop() {
    this.running = false;
    this.value = 0;
  }

  public boolean check() {
    if (!running) return false;
    long elapsed = Duration.between(startTime, Instant.now()).getSeconds();
    return elapsed > 0; // Логика проверки
  }
}
