package com.williamhill.whgtf.testra.jvm.client.api;

import static com.williamhill.whgtf.testra.jvm.util.JsonObject.jsonToList;
import static com.williamhill.whgtf.testra.jvm.util.JsonObject.objectToJson;
import static com.williamhill.whgtf.testra.jvm.util.PropertyHelper.prop;
import static com.williamhill.whgtf.testra.jvm.util.StringHelper.substitute;

import com.williamhill.whgtf.test.bnw.pojo.executions.ExecutionRequest;
import com.williamhill.whgtf.test.bnw.pojo.project.ProjectRequest;
import com.williamhill.whgtf.test.bnw.pojo.project.ProjectResponse;
import com.williamhill.whgtf.test.bnw.pojo.scenarios.ScenarioRequest;
import com.williamhill.whgtf.test.bnw.pojo.testresult.TestResultRequest;
import com.williamhill.whgtf.testra.jvm.client.http.GenericHttpClient;
import com.williamhill.whgtf.testra.jvm.message.http.HttpRequestMessage;
import com.williamhill.whgtf.testra.jvm.message.http.HttpResponseMessage;
import com.williamhill.whgtf.testra.jvm.util.JsonObject;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TestraRestClient {

  private static final Logger LOGGER;
  private static final String TESTRA_HOST= prop("host");
  private static final GenericHttpClient httpClient;
  private static final String PROJECTID = "projectID";
  private static final String EXECUTIONID = "executionID";
  private static String projectIDString;

  static {

    LOGGER = LoggerFactory.getLogger(TestraRestClient.class);
    httpClient =
        new GenericHttpClient(
            "TestraRestClient", (Integer.parseInt(prop("beConnectionPoolSize", "0"))));
  }

  public String getProjectID(String projectName){
    HttpResponseMessage httpResponseMessage =
        httpClient.get(defaultHttpRequestMessage()
        .withUrl(TESTRA_HOST + prop("getProjects")));
    List<ProjectResponse> projectResponses = jsonToList(httpResponseMessage.getPayload(), ProjectResponse.class);
    try {
      String id =  projectResponses.stream()
          .filter(p -> p.getName().equals(projectName))
          .collect(Collectors.toList())
          .get(0).getId();
      projectIDString = id;
      return id;
    }
    catch(IndexOutOfBoundsException e){
      LOGGER.error("No project found with the name projectName, creating..");
      httpResponseMessage = httpClient.post(defaultHttpRequestMessage()
      .withUrl(TESTRA_HOST + prop("getProjects"))
      .withPayload(objectToJson(new ProjectRequest().withName(projectName))));
      projectIDString = JsonObject.jsonToObject(httpResponseMessage.getPayload(), ProjectResponse.class)
          .getId();
      return  projectIDString;
    }
  }

  public void deleteProject(String projectID){
      HttpResponseMessage httpResponseMessage = httpClient
          .delete(defaultHttpRequestMessage()
          .withUrl(TESTRA_HOST + substitute(prop("deleteProject"),PROJECTID,projectID)));
  }

  public void createProject(String projectName){
    HttpResponseMessage httpResponseMessage = httpClient
        .post(defaultHttpRequestMessage()
        .withUrl(prop("getProjects"))
        .withPayload(JsonObject.objectToJson(new ProjectRequest().withName(projectName))));
  }

  public HttpResponseMessage addScenario(ScenarioRequest scenarioRequest){
    return  httpClient.post(defaultHttpRequestMessage()
        .withUrl(TESTRA_HOST+ substitute(prop("getScenarios"), PROJECTID, projectIDString))
            .withPayload(JsonObject.objectToJson(scenarioRequest)));
  }

  public HttpResponseMessage getScenarios(){
    return  httpClient.get(defaultHttpRequestMessage()
        .withUrl(TESTRA_HOST+ substitute(prop("getScenarios"), PROJECTID, projectIDString)));
  }

  public HttpResponseMessage createExecution()
  {
    return httpClient.post(defaultHttpRequestMessage()
    .withUrl(TESTRA_HOST + substitute(prop("getExecutions"), PROJECTID, projectIDString))
    .withPayload(JsonObject.objectToJson(new ExecutionRequest().withIsParallel(false))));
  }

  public HttpResponseMessage addTestResult(TestResultRequest testResultRequest, String executionID){
    return httpClient.post(defaultHttpRequestMessage()
    .withUrl(TESTRA_HOST + substitute(substitute(prop("getTestResults"),EXECUTIONID,executionID),PROJECTID,projectIDString))
    .withPayload(JsonObject.objectToJson(testResultRequest)));

  }


  private HttpRequestMessage defaultHttpRequestMessage() {
    return new HttpRequestMessage()
        .withJsonContentType();
  }

}
