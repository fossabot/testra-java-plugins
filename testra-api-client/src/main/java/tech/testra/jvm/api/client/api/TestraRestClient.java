package tech.testra.jvm.api.client.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.testra.jvm.api.client.http.GenericHttpClient;
import tech.testra.jvm.api.client.models.executions.ExecutionRequest;
import tech.testra.jvm.api.client.models.project.ProjectRequest;
import tech.testra.jvm.api.client.models.project.ProjectResponse;
import tech.testra.jvm.api.client.models.scenarios.ScenarioRequest;
import tech.testra.jvm.api.client.models.testresult.TestResultRequest;
import tech.testra.jvm.api.message.http.HttpRequestMessage;
import tech.testra.jvm.api.message.http.HttpResponseMessage;
import tech.testra.jvm.api.util.JsonObject;

import java.util.List;
import java.util.stream.Collectors;

import static tech.testra.jvm.api.util.HttpAssertHelper.assertHttpStatusAsCreated;
import static tech.testra.jvm.api.util.HttpAssertHelper.assertHttpStatusAsOk;
import static tech.testra.jvm.api.util.JsonObject.jsonToList;
import static tech.testra.jvm.api.util.JsonObject.objectToJson;
import static tech.testra.jvm.api.util.PropertyHelper.prop;
import static tech.testra.jvm.api.util.StringHelper.substitute;

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
    assertHttpStatusAsCreated(httpResponseMessage);
  }

  public HttpResponseMessage addScenario(ScenarioRequest scenarioRequest){
    HttpResponseMessage httpResponseMessage = httpClient.post(defaultHttpRequestMessage()
        .withUrl(TESTRA_HOST+ substitute(prop("getScenarios"), PROJECTID, projectIDString))
            .withPayload(JsonObject.objectToJson(scenarioRequest)));
    assertHttpStatusAsCreated(httpResponseMessage);
    return httpResponseMessage;
  }

  public HttpResponseMessage getScenarios(){
    return  httpClient.get(defaultHttpRequestMessage()
        .withUrl(TESTRA_HOST+ substitute(prop("getScenarios"), PROJECTID, projectIDString)));
  }

  public HttpResponseMessage createExecution()
  {
    HttpResponseMessage httpResponseMessage = httpClient.post(defaultHttpRequestMessage()
    .withUrl(TESTRA_HOST + substitute(prop("getExecutions"), PROJECTID, projectIDString))
    .withPayload(JsonObject.objectToJson(new ExecutionRequest().withIsParallel(false))));
    assertHttpStatusAsOk(httpResponseMessage);
    return httpResponseMessage;
  }

  public HttpResponseMessage addTestResult(TestResultRequest testResultRequest, String executionID){
    HttpResponseMessage httpResponseMessage = httpClient.post(defaultHttpRequestMessage()
    .withUrl(TESTRA_HOST + substitute(substitute(prop("getTestResults"),EXECUTIONID,executionID),PROJECTID,projectIDString))
    .withPayload(JsonObject.objectToJson(testResultRequest)));
    assertHttpStatusAsCreated(httpResponseMessage);
    return httpResponseMessage;
  }


  private HttpRequestMessage defaultHttpRequestMessage() {
    return new HttpRequestMessage()
        .withJsonContentType();
  }

}
