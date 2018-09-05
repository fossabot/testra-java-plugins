package tech.testra.jvm.gatling.plugin.parseUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import tech.testra.jvm.gatling.plugin.enums.LineType;

/**
 * Made for Gatling 2.3.1
 */

public class SimParser {

  private File file;
  private SimStats stats;

  public SimParser(File file) {
    this.file = file;
  }

  public SimStats parse() {
    try {
      stats = new SimStats(file);
      BufferedReader br = new BufferedReader(new FileReader(file.getAbsolutePath()));
      long lines = 0;
      while (br.readLine() != null)
        lines++;
      stats.setLineCount(lines);
      SimReader simReader = new SimReader(file);
      List<String> lineBreakdown;
      String name, scenario;
      long start, end;
      boolean success;
      List<String> header = simReader.readNext();
      processHeader(header);
      while ((lineBreakdown = simReader.readNext()) != null) {
        scenario = getScenario(lineBreakdown);
        switch (getType(lineBreakdown)) {
          case RUN:
            break;
          case USER:
            switch (LineType.valueOf(getUserType(lineBreakdown))) {
              case START:
                stats.addUser(scenario);
                break;
              case END:
                stats.endUser(scenario);
                break;
            }
            break;
          case REQUEST:
            name = getRequestName(lineBreakdown);
            start = getRequestStart(lineBreakdown);
            end = getRequestEnd(lineBreakdown);
            success = getRequestSuccess(lineBreakdown);
            stats.addRequest(scenario, name, start, end, success);
            break;
        }
      }
      stats.computeStat();
      return stats;
    }
    catch(IOException e){
      e.printStackTrace();
      return null;
    }
  }




  private void processHeader(List<String> header){
    if(header.size() <3){
      throw new IllegalArgumentException("Invalid simulation file for gatling formatter version 2.3.1");
    }
    stats.setSimulationName(getSimulationName(header));
    stats.setScenarioName(getScenario(header));
    stats.setStartTime(getSimulationStart(header));
  }

  protected String getSimulationName(List<String> line) {
    return line.get(3);
  }

  protected long getSimulationStart(List<String> line) {
    return Long.parseLong(line.get(4));
  }

  protected String getScenario(List<String> line) {
    return line.get(1);
  }

  protected LineType getType(List<String> line) {
    return LineType.valueOf(line.get(0));
  }

  protected String getUserType(List<String> line) {
    return line.get(3);
  }

  protected String getRequestName(List<String> line) {
    return line.get(4);
  }

  protected Long getRequestStart(List<String> line) {
    return Long.parseLong(line.get(5));
  }

  protected Long getRequestEnd(List<String> line) {
    return Long.parseLong(line.get(6));
  }

  protected boolean getRequestSuccess(List<String> line) {
    return LineType.OK.getValue().equals(line.get(7));
  }

}
