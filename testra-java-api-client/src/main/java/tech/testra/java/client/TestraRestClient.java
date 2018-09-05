package tech.testra.java.client;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.testra.java.client.api.ExecutionApi;
import tech.testra.java.client.api.ProjectApi;
import tech.testra.java.client.api.ResultApi;
import tech.testra.java.client.api.ScenarioApi;
import tech.testra.java.client.api.SimulationApi;
import tech.testra.java.client.api.TestcaseApi;
import tech.testra.java.client.model.*;
import tech.testra.java.client.model.TestResultRequest.StatusEnum;
import tech.testra.java.client.utils.HostNameUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static tech.testra.jvm.commons.util.PropertyHelper.prop;


public final class TestraRestClient {

  private static final Logger LOGGER = LoggerFactory.getLogger(TestraRestClient.class);
  private static String projectIDString;
  private static String executionIDString;
  private static ProjectApi projectApi = new ProjectApi();
  private static ScenarioApi scenarioApi = new ScenarioApi();
  private static ExecutionApi executionApi = new ExecutionApi();
  private static ResultApi resultApi = new ResultApi();
  private static SimulationApi simulationtApi = new SimulationApi();
  private static TestcaseApi testcaseApi = new TestcaseApi();
  public static String buildRef;
  public static String executionDescription;

    public TestraRestClient() {
    projectApi.getApiClient().setDebugging(true);
  }

    public static void setURLs(String url) {
    projectApi.getApiClient().setBasePath(url);
    executionApi.getApiClient().setBasePath(url);
    resultApi.getApiClient().setBasePath(url);
    scenarioApi.getApiClient().setBasePath(url);
    if (Boolean.parseBoolean(prop("testra.debug"))) {
      projectApi.getApiClient().setDebugging(true);
      executionApi.getApiClient().setDebugging(true);
      resultApi.getApiClient().setDebugging(true);
      scenarioApi.getApiClient().setDebugging(true);
      testcaseApi.getApiClient().setDebugging(true);
    }
  }


    public static String getProjectIDFromList(String projectName) {
    List<Project> projects = new ArrayList<>();
    try {
        projects = projectApi.getProjects();
    } catch (ApiException e) {
      e.printStackTrace();
    }
        if (projects.stream().anyMatch(x -> x.getName().equals(projectName))) {
            projectIDString =
                    projects
                            .stream()
                            .filter(x -> x.getName().equals(projectName))
                            .collect(Collectors.toList())
                            .get(0)
                            .getId();
      return projectIDString;
        } else {
      LOGGER.error("No project found with name " + projectName);
      throw new IllegalArgumentException("Unknown project");
    }
  }

  public static String getProjectID(String projectName){
      try {
        projectIDString = projectApi.getProject(projectName).getId();
        LOGGER.info("Project ID found");
        return projectIDString;
      } catch (ApiException e) {
        e.printStackTrace();
        throw new IllegalArgumentException("Unknown project");
      }
  }

    public static Scenario createScenario(ScenarioRequest scenarioRequest) {
    try {
        Scenario scenario = scenarioApi.createScenario(projectIDString, scenarioRequest);
      return scenario;
    } catch (ApiException e) {
      LOGGER.error("Error Creating Scenario " + scenarioRequest.getName());
      LOGGER.error(e.getResponseBody());
      e.printStackTrace();
      return null;
    }
  }

  public static Testcase createTestcase(TestcaseRequest testcaseRequest) {
    try {
      Testcase testcase = testcaseApi.createTestcase(projectIDString, testcaseRequest);
      return testcase;
    } catch (ApiException e) {
      LOGGER.error("Error Creating Test Case " + testcaseRequest.getName());
      LOGGER.error(e.getResponseBody());
      e.printStackTrace();
      return null;
    }
  }

    private static synchronized String createExecution() {
      if(executionIDString != null){
        return executionIDString;
      }
    ExecutionRequest executionRequest = new ExecutionRequest();
    executionRequest.setParallel(false);
    if(prop("branch")!= null)
      executionRequest.setBranch(prop("branch"));
    if(prop("testra.environment")!=null)
      executionRequest.setEnvironment(prop("testra.environment"));
    if(executionDescription!= null){
      executionRequest.setDescription(executionDescription);
    }
    if(buildRef!= null){
      executionRequest.buildRef(buildRef);
    }
      executionRequest.setHost(HostNameUtil.hostName());
    executionRequest.setTags(Collections.singletonList(""));
    try {
        Execution execution = executionApi.createExecution(projectIDString, executionRequest);
      executionIDString = execution.getId();
      return executionIDString;
    } catch (ApiException e) {
      LOGGER.error("Error Creating Execution");
      LOGGER.error(e.getResponseBody());
      e.printStackTrace();
      return null;
    }
  }

    public static TestResult createResult(TestResultRequest testResultRequest) {
    try {
        return resultApi.createResult(projectIDString, executionIDString, testResultRequest);
    } catch (ApiException e) {
      LOGGER.error("Error Creating Result " + testResultRequest.getTargetId());
      LOGGER.error(e.getResponseBody());
      e.printStackTrace();
      return null;
    }
  }

  public static TestResult updateResult(String resultID, TestResultRequest testResultRequest){
    try {
      return resultApi.updateResult(projectIDString,executionIDString,resultID,testResultRequest);
    } catch (ApiException e) {
      e.printStackTrace();
      throw new IllegalArgumentException("Could not update result");
    }
  }

  public static synchronized void setExecutionid(String eid){
      if(eid == null){
        createExecution();
      }
      else {
        executionIDString = eid;
      }
  }

  public static String getExecutionid(){
      return executionIDString;
  }

  public static List<EnrichedTestResult> getFailedResults(){
      return getResults(StatusEnum.FAILED.toString());
  }

  public static List<EnrichedTestResult> getResults(String resultType){
    try {
      return resultApi.getResults(projectIDString,executionIDString,resultType);
    } catch (ApiException e) {
      e.printStackTrace();
      throw new IllegalArgumentException("No results found with execution ID " + executionIDString);
    }
  }

  public static void createExecutionIDFile(){
    File file = new File("testra.exec");
    FileWriter writer = null;
    try {
      if (file.createNewFile()){
        System.out.println("File is created!");
      }else{
        System.out.println("File already exists.");
      }
      writer = new FileWriter(file);
      writer.write(TestraRestClient.getExecutionid());
      writer.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void createSimulation(SimulationRequest simulationRequest){
    try {
      simulationtApi.createSimulation(projectIDString, executionIDString, simulationRequest);
    } catch (ApiException e) {
      LOGGER.error("Error Creating Simulation");
      LOGGER.error(e.getResponseBody());
      e.printStackTrace();
    }
  }
}
