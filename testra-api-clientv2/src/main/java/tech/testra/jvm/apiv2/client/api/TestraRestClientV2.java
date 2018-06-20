package tech.testra.jvm.apiv2.client.api;


import static tech.testra.jvm.apiv2.util.PropertyHelper.prop;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.testra.jvm.client.ApiException;
import tech.testra.jvm.client.api.ExecutionApi;
import tech.testra.jvm.client.api.ProjectApi;
import tech.testra.jvm.client.api.ResultApi;
import tech.testra.jvm.client.api.ScenarioApi;
import tech.testra.jvm.client.model.Execution;
import tech.testra.jvm.client.model.ExecutionRequest;
import tech.testra.jvm.client.model.Project;
import tech.testra.jvm.client.model.ProjectRequest;
import tech.testra.jvm.client.model.Scenario;
import tech.testra.jvm.client.model.ScenarioRequest;
import tech.testra.jvm.client.model.TestResult;
import tech.testra.jvm.client.model.TestResultRequest;

public final class TestraRestClientV2 {

  private static final Logger LOGGER = LoggerFactory.getLogger(TestraRestClientV2.class);
  private static final String TESTRA_HOST= prop("host");
  private static final String PROJECTID = "projectID";
  private static final String EXECUTIONID = "executionID";
  private static String projectIDString;
  private static String executionIDString;
  private ProjectApi projectApi = new ProjectApi();
  private ScenarioApi scenarioApi = new ScenarioApi();
  private ExecutionApi executionApi = new ExecutionApi();
  private ResultApi resultApi = new ResultApi();



  public TestraRestClientV2(){
    projectApi.getApiClient().setDebugging(true);
  }


  public void setURLs(String url){
    projectApi.getApiClient().setBasePath(url);
    executionApi.getApiClient().setBasePath(url);
    resultApi.getApiClient().setBasePath(url);
    scenarioApi.getApiClient().setBasePath(url);
  }

  public String getProjectID(String projectName){
    List<Project> projects = new ArrayList<>();
    try {
      projects = projectApi.projectsGet();
    } catch (ApiException e) {
      e.printStackTrace();
    }
    if(projects.stream().anyMatch(x -> x.getName().equals(projectName))){
      projectIDString = projects.stream().filter(x -> x.getName().equals(projectName)).collect(
          Collectors.toList()).get(0).getId();
      return projectIDString;
    }
    else{
      LOGGER.error("No project found with name " + projectName);
      throw new IllegalArgumentException("Unknown project");
    }
  }

  public String createScenario(ScenarioRequest scenarioRequest){
    try {
      Scenario scenario = scenarioApi.projectsProjectIdScenariosPost(projectIDString, scenarioRequest);
      return scenario.getId();
    } catch (ApiException e) {
      LOGGER.error("Error Creating Scenario " + scenarioRequest.getName());
      LOGGER.error(e.getResponseBody());
      e.printStackTrace();
      return null;
    }
  }

  public String createExecution(){
    ExecutionRequest executionRequest = new ExecutionRequest();
    executionRequest.setIsParallel(false);
    try {
      Execution execution = executionApi.projectsProjectIdExecutionsPost(projectIDString, executionRequest);
      executionIDString = execution.getId();
      return executionIDString;
    } catch (ApiException e) {
      LOGGER.error("Error Creating Execution");
      LOGGER.error(e.getResponseBody());
      e.printStackTrace();
      return null;
    }
  }

  public void createResult(TestResultRequest testResultRequest){
    try {
      resultApi.projectsProjectIdExecutionsExecutionIdResultsPost(projectIDString, executionIDString, testResultRequest);
    } catch (ApiException e) {
      LOGGER.error("Error Creating Result " + testResultRequest.getTargetId());
      LOGGER.error(e.getResponseBody());
      e.printStackTrace();
    }
  }

}
