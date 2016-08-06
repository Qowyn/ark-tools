package qowyn.ark.tools;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class Stopwatch {

  private final boolean enabled;

  private final Instant start;

  private final List<Entry> stops;

  public Stopwatch(boolean enabled) {
    this.enabled = enabled;
    if (enabled) {
      this.stops = new ArrayList<>();
      this.start = Instant.now();
    } else {
      this.stops = null;
      this.start = null;
    }
  }

  public void stop(String description) {
    if (enabled) {
      Entry entry = new Entry();
      entry.description = description;
      entry.pointInTime = Instant.now();
      stops.add(entry);
    }
  }

  public void print() {
    if (!enabled || stops.size() < 1) {
      return;
    }

    StringBuilder sb = new StringBuilder(40 * (stops.size() + 1));

    Instant startPoint = start;
    for (Entry next : stops) {
      sb.append(next.description);
      sb.append(" finished after");
      sb.append(ChronoUnit.MILLIS.between(startPoint, next.pointInTime));
      sb.append(" ms\n");
      startPoint = next.pointInTime;
    }

    if (stops.size() > 1) {
      sb.append("Total time ");
      sb.append(ChronoUnit.MILLIS.between(start, stops.get(stops.size() - 1).pointInTime));
      sb.append(" ms\n");
    }

    System.out.print(sb.toString());
  }

  private static class Entry {

    public String description;

    public Instant pointInTime;

  }

}
