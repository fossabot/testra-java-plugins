package tech.testra.jvm.gatling.plugin.parseUtils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Data;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

@Data

public class RequestStats {

  private static final AtomicInteger statCounter = new AtomicInteger();
  private String simulation, startDate;
  private String scenario;
  private final String request;
  private final String requestId;
  private double duration;
  private final List<Double> durations;
  private final int indice;
  private long count, successCount, errorCount, start, end;
  private Map<Integer, Double> percentiles = new HashMap<>();
  private long min, max;
  private double rps, avg, standardDeviation;
  private int maxUsers;

  public RequestStats(String simulation, String scenario, String request, long start) {
    this.simulation = simulation;
    this.scenario = scenario;
    this.request = request;
    requestId = getIdentifier(request);
    this.start = start;
    durations = new ArrayList<>();
    indice = statCounter.incrementAndGet();
  }

  public static String getIdentifier(String str) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < str.length(); i++) {
      if (Character.isLetter(str.charAt(i)))
        sb.append(str.charAt(i));
    }
    return sb.toString().toLowerCase();
  }

  public void add(long start, long end, boolean success) {
    count += 1;
    if (this.start == 0) {
      this.start = start;
    }
    this.start = Math.min(this.start, start);
    this.end = Math.max(this.end, end);
    if (!success) {
      errorCount += 1;
    }
    long duration = end - start;
    durations.add((double) duration);
  }

  public void calculateStats(int maxUsers) {
    calculateStats((end - start) / 1000.0, maxUsers);
  }

  public void calculateStats(double duration, Integer maxUsers){
    double[] durs = getDurationAsArray();
    min = (long) StatUtils.min(durs);
    max = (long) StatUtils.max(durs);
    double sum = 0;
    for (double d : durs)
      sum += d;
    avg = sum / durs.length;
    percentiles.put(50, StatUtils.percentile(durs, 50.0));
    percentiles.put(90, StatUtils.percentile(durs, 90.0));
    percentiles.put(95, StatUtils.percentile(durs, 95.0));
    percentiles.put(99, StatUtils.percentile(durs, 99.0));
    StandardDeviation stdDev = new StandardDeviation();
    standardDeviation = stdDev.evaluate(durs, avg);
    this.duration = duration;
    this.maxUsers = maxUsers;
    rps = (count - errorCount) / duration;
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd " + "HH:mm:ss")
        .withZone(ZoneId.systemDefault());
    startDate = formatter.format(Instant.ofEpochMilli(start));
    successCount = count - errorCount;

  }

  private double[] getDurationAsArray() {
    double[] ret = new double[durations.size()];
    for (int i = 0; i < durations.size(); i++) {
      ret[i] = durations.get(i);
    }
    return ret;
  }

}
