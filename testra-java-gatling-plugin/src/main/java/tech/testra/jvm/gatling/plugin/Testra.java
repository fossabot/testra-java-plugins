package tech.testra.jvm.gatling.plugin;


import static tech.testra.jvm.commons.util.PropertyHelper.prop;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.testra.java.client.TestraRestClient;
import tech.testra.java.client.model.Percentile;
import tech.testra.java.client.model.SimulationRequest;
import tech.testra.java.client.model.SimulationScenario;
import tech.testra.jvm.commons.util.CommonData;
import tech.testra.jvm.commons.util.CommonDataProvider;
import tech.testra.jvm.commons.util.PropertyHelper;
import tech.testra.jvm.gatling.plugin.parseUtils.RequestStats;
import tech.testra.jvm.gatling.plugin.parseUtils.SimParser;
import tech.testra.jvm.gatling.plugin.parseUtils.SimStats;

public class Testra {

  private static final Logger LOGGER = LoggerFactory.getLogger(Testra.class);
  private final Long threadId = Thread.currentThread().getId();
  private CommonData commonData;
  private String projectID;
  private String executionID;
  private SimParser parser;

  public Testra(){
    PropertyHelper.loadPropertiesFromAbsolute(new File(".testra").getAbsolutePath());
    setup();
    TestraRestClient.setURLs(prop("host"));
    projectID = TestraRestClient.getProjectID(prop("project"));
    LOGGER.info("Project ID is " + projectID);
    commonData.isRetry = Boolean.parseBoolean(prop("isrerun"));
    commonData.setExecutionID=Boolean.parseBoolean(prop("setExecutionID"));
    if(prop("buildRef")!=null){
      TestraRestClient.buildRef = prop("buildRef");
    }
    if(prop("testra.execution.description")!=null){
      TestraRestClient.executionDescription = prop("testra.execution.description");
    }
    createExecution();
  }

  private synchronized void createExecution() {
    if(TestraRestClient.getExecutionid() == null) {
      if(commonData.isRetry){
        File file = new File("testra.exec");
        if(file.isFile()){
          try {
            Scanner fileReader = new Scanner(file);
            TestraRestClient.setExecutionid(fileReader.nextLine());
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
        else {
          TestraRestClient.setExecutionid(prop("previousexecutionID"));
        }
      }
      else if(commonData.setExecutionID){
        TestraRestClient.setExecutionid(prop("previousexecutionID"));
      }
      else {
        TestraRestClient.setExecutionid(null);
        TestraRestClient.createExecutionIDFile();
      }
    }
    executionID = TestraRestClient.getExecutionid();
    LOGGER.info("Execution ID is: " + executionID);
    LOGGER.info(prop("host") + "/projects/" + projectID + "/executions/"+ executionID+"\n");
  }

  private void setup(){
    LOGGER.info("Scenario setup - Begin");
    CommonDataProvider.newCommonData(threadId);
    if ((commonData = CommonDataProvider.get(threadId)) == null) {
      throw new RuntimeException("Common data not found for id : " + threadId);
    }
    else {
      LOGGER.info("Initialised common data instance for scenario");
    }
    LOGGER.info("Scenario setup - End");
  }

  public static void main(String[] args){
    Testra testra = new Testra();
//    String logString = "testra-java-gatling-plugin/sim.log";
    String logString = "/Users/pajohnson/Downloads/walletservicesimulation-1534423154351/simulation.log";
    if(args.length ==0)
      testra.processLog(logString);
    else{
      for(String s : args){
        testra.processLog(s);
      }
    }
    LOGGER.info("Parsing complete");
  }

  public void processLog(String logLocation){
    parseFile(new File(logLocation));
  }

  private void parseFile(File file){
    LOGGER.info("Parsing log file " + file.getAbsolutePath());
      parser = new SimParser(file);
      SimStats stats = parser.parse();
      postStats(stats);
  }

  private void postStats(SimStats stats){
    SimulationRequest simulationRequest = new SimulationRequest();
    simulationRequest.setName(stats.getSimulationName());
    simulationRequest.setNamespace(stats.getScenarioName());
    List<SimulationScenario> simulationScenarios = new ArrayList<>();
    stats.getReqStats().keySet().forEach(reqName ->{
      stats.getReqStats().get(reqName);
      simulationScenarios.add(buildScenario(stats.getReqStats().get(reqName), reqName));
    });
    simulationScenarios.add(buildScenario(stats.getRequestStats(), "_all"));
    simulationRequest.setScenarios(simulationScenarios);
    TestraRestClient.createSimulation(simulationRequest);
  }

  private SimulationScenario buildScenario(RequestStats request, String reqName){
    SimulationScenario simulationScenario = new SimulationScenario();
    simulationScenario.setRequest(reqName);
    simulationScenario.setAverage(new BigDecimal(request.getAvg()));
    simulationScenario.setAvgRequestPerSec((int)Math.round(request.getRps()));
    simulationScenario.setCount(request.getCount());
    simulationScenario.setDurationInMs((long)request.getDuration());
    simulationScenario.setEndTime(request.getEnd());
    simulationScenario.setErrorCount(request.getErrorCount());
    simulationScenario.setMin(new BigDecimal(request.getMin()));
    simulationScenario.setMax(new BigDecimal(request.getMax()));
    simulationScenario.setStartTime(request.getStart());
    simulationScenario.setStdDiv(new BigDecimal(request.getStandardDeviation()));
    simulationScenario.setSuccessCount(request.getSuccessCount());
    List<Percentile> percentiles = new ArrayList<>();
    request.getPercentiles().keySet().forEach(p -> {
      Percentile percentile = new Percentile();
      percentile.setN(p);
      percentile.setValue(new BigDecimal(request.getPercentiles().get(p)));
      percentiles.add(percentile);
    });
    simulationScenario.setPercentiles(percentiles);
    return simulationScenario;
  }

}
