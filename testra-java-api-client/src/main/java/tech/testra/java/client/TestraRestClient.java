package tech.testra.java.client;

import static tech.testra.jvm.commons.util.PropertyHelper.prop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.testra.java.client.api.ExecutionApi;
import tech.testra.java.client.api.ProjectApi;
import tech.testra.java.client.api.ResultApi;
import tech.testra.java.client.api.ScenarioApi;
import tech.testra.java.client.model.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


public final class TestraRestClient {

  private static final Logger LOGGER = LoggerFactory.getLogger(TestraRestClient.class);
  private static String projectIDString;
  private static String executionIDString;
  private static ProjectApi projectApi = new ProjectApi();
  private static ScenarioApi scenarioApi = new ScenarioApi();
  private static ExecutionApi executionApi = new ExecutionApi();
  private static ResultApi resultApi = new ResultApi();
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
    if(Boolean.parseBoolean(prop("debug"))) {
      projectApi.getApiClient().setDebugging(true);
      executionApi.getApiClient().setDebugging(true);
      resultApi.getApiClient().setDebugging(true);
      scenarioApi.getApiClient().setDebugging(true);
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
        return projectIDString;
      } catch (ApiException e) {
        e.printStackTrace();
        throw new IllegalArgumentException("Unknown project");
      }
  }

    public static String createScenario(ScenarioRequest scenarioRequest) {
    try {
        Scenario scenario = scenarioApi.createScenario(projectIDString, scenarioRequest);
      return scenario.getId();
    } catch (ApiException e) {
      LOGGER.error("Error Creating Scenario " + scenarioRequest.getName());
      LOGGER.error(e.getResponseBody());
      e.printStackTrace();
      return null;
    }
  }

    public static String createExecution() {
    ExecutionRequest executionRequest = new ExecutionRequest();
    executionRequest.setIsParallel(false);
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
    executionRequest.setHost(prop("host"));
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

    public static void createResult(TestResultRequest testResultRequest) {
    try {
        resultApi.createResult(projectIDString, executionIDString, testResultRequest);
    } catch (ApiException e) {
      LOGGER.error("Error Creating Result " + testResultRequest.getTargetId());
      LOGGER.error(e.getResponseBody());
      e.printStackTrace();
    }
  }

  public static void updateResult(String resultID, TestResultRequest testResultRequest){
    try {
      resultApi.updateResult(projectIDString,executionIDString,resultID,testResultRequest);
    } catch (ApiException e) {
      e.printStackTrace();
      throw new IllegalArgumentException("Could not update result");
    }
  }

  public static void setExecutionid(String eid){
      executionIDString = eid;
  }

  public static String getExecutionid(){
      return executionIDString;
  }

  public static List<TestResult> getFailedResults(){
      return getResults(Result.FAILED.getValue());
  }

  public static List<TestResult> getResults(String resultType){
    try {
      return resultApi.getResults(projectIDString,executionIDString,resultType);
    } catch (ApiException e) {
      e.printStackTrace();
      throw new IllegalArgumentException("No results found with execution ID " + executionIDString);
    }
  }
}
