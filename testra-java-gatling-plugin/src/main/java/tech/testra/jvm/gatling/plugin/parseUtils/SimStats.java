package tech.testra.jvm.gatling.plugin.parseUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import javafx.util.Pair;
import lombok.Data;
import static java.lang.Math.max;

@Data
public class SimStats {

  private File file;
  private long lineCount;
  private String simulationName;
  private String scenarioName;
  private long start;
  private final Map<String, Pair<Integer,Integer>> users = new HashMap<>();
  private final Map<String, RequestStats> reqStats = new HashMap<>();
  private final RequestStats requestStats;
  private int maxUsers;

  public SimStats(File file){
    this.file = file;
    this.requestStats = new RequestStats("all","all","all", 0);
  }

  public void setStartTime(long start) {
    this.start = start;
    requestStats.setStart(start);
  }

  public void addUser(String scenario){
    Pair<Integer, Integer> userCount = users.computeIfAbsent(scenario, x -> new Pair<>(0,0));
    users.put(scenario, new Pair<>(userCount.getKey()+1, max(userCount.getKey()+1, userCount.getValue())));
  }

  public void endUser(String scenario){
    users.put(scenario, new Pair<>(users.get(scenario).getKey()-1, users.get(scenario).getValue()));
  }

  public void addRequest(String scenario, String requestName, long start, long end, boolean success) {
    RequestStats request = reqStats.computeIfAbsent(requestName,
        n -> new RequestStats(simulationName, scenario, n, this.start));
    request.add(start, end, success);
    requestStats.add(start, end, success);

  }

  public void computeStat() {
    maxUsers = users.values().stream().mapToInt(Pair::getValue).sum();
    requestStats.calculateStats(maxUsers);
    reqStats.values()
        .forEach(request -> request.calculateStats(requestStats.getDuration(), users.get(request.getScenario()).getValue()));

  }

}
