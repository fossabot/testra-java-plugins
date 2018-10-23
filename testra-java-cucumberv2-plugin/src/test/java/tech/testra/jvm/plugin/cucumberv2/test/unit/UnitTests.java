package tech.testra.jvm.plugin.cucumberv2.test.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cucumber.api.Result;
import cucumber.api.Result.Type;
import cucumber.api.TestCase;
import cucumber.api.TestStep;
import cucumber.api.event.TestCaseFinished;
import cucumber.api.event.TestCaseStarted;
import cucumber.runner.PickleTestStep;
import cucumber.runtime.DefinitionMatch;
import cucumber.runtime.StepDefinition;
import cucumber.runtime.StepDefinitionMatch;
import gherkin.ast.Feature;
import gherkin.events.PickleEvent;
import gherkin.pickles.Pickle;
import gherkin.pickles.PickleLocation;
import gherkin.pickles.PickleStep;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.powermock.reflect.Whitebox;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import tech.testra.java.client.ApiException;
import tech.testra.java.client.TestraRestClient;
import tech.testra.java.client.api.ExecutionApi;
import tech.testra.java.client.api.ProjectApi;
import tech.testra.java.client.api.ResultApi;
import tech.testra.java.client.api.ScenarioApi;
import tech.testra.java.client.model.Execution;
import tech.testra.java.client.model.ExecutionRequest;
import tech.testra.java.client.model.Project;
import tech.testra.java.client.model.Scenario;
import tech.testra.java.client.model.ScenarioRequest;
import tech.testra.java.client.model.TestResult;
import tech.testra.java.client.model.TestResultRequest;
import tech.testra.java.client.utils.HostNameUtil;
import tech.testra.jvm.commons.util.MD5;
import tech.testra.jvm.plugin.cucumberv2.Testra;
import tech.testra.jvm.plugin.cucumberv2.utils.CommonData;
import tech.testra.jvm.plugin.cucumberv2.utils.CucumberSourceUtils;

public class UnitTests {

  private String projectID = "5b45e66a52faff0001d7a368";
  private String executionID = "5b45e66a52faff1231d7a368";
  Project project = new Project();
  ExecutionRequest executionRequest = new ExecutionRequest();
  Testra testra;

  @BeforeTest
  public void setup() throws ApiException {
    ProjectApi projectApi = mock(ProjectApi.class);

    project.setId(projectID);
    project.setName("Mock");
    project.setCreationDate(1234567L);
    project.setDescription("Mock descrition");
    when(projectApi.getProject("Mock")).thenReturn(project);
    Whitebox.setInternalState(TestraRestClient.class, "projectApi", projectApi);

    ExecutionApi executionApi = mock(ExecutionApi.class);
    Execution execution = new Execution();
    execution.setBranch("branchname");
    execution.setBuildRef("buildref");
    execution.setDescription("description");
    execution.setEndTime(1234L);
    execution.setStartTime(234L);
    execution.setEnvironment("testEnv");
    execution.setHost("localhost:8080");
    execution.setId(executionID);

    executionRequest.setParallel(false);
    executionRequest.setBranch("branchname");
    executionRequest.setEnvironment("testEnv");
    executionRequest.setDescription("description");
    executionRequest.buildRef("buildRef");
    executionRequest.setHost(HostNameUtil.hostName());
    executionRequest.setTags(Collections.singletonList(""));

    when(executionApi.createExecution(eq(projectID),any(ExecutionRequest.class))).thenReturn(execution);
    Whitebox.setInternalState(TestraRestClient.class, "executionApi", executionApi);

    Scenario scenario = new Scenario();
    scenario.setId("idStringForScenario");
    scenario.setFeatureId("idStringForFeature");
    ScenarioApi scenarioApi = mock(ScenarioApi.class);
    when(scenarioApi.createScenario(eq(projectID), any(ScenarioRequest.class))).thenReturn(scenario);
    Whitebox.setInternalState(TestraRestClient.class, "scenarioApi", scenarioApi);

    TestResult testResult = new TestResult();
    testResult.setId("idStringForResult");
    ResultApi resultApi = mock(ResultApi.class);
    Whitebox.setInternalState(TestraRestClient.class, "resultApi", resultApi);
    when(resultApi.createResult(eq(projectID),eq(executionID),any(TestResultRequest.class))).thenReturn(testResult);

  }


  public void getProjectID() throws ApiException {
      String id = TestraRestClient.getProjectID("Mock");
      assertThat(id).as("id was " + id + " should have been " +  project.getId())
      .isEqualTo(project.getId());
  }


  public void createExecution() throws Exception {
    Testra testra = mock(Testra.class);
    Whitebox.setInternalState(testra, "commonData", createCommonData());
    TestraRestClient.getProjectID("Mock");
    Whitebox.invokeMethod(testra, "createExecution");
    assertThat(TestraRestClient.getExecutionid()).isEqualTo(executionID);
  }

  public void createScenario() throws Exception {
    prepTestraRestClient();
    testra = mock(Testra.class);
    Whitebox.setInternalState(testra, "commonData", createCommonData());
    Whitebox.invokeMethod(testra, "handleTestCaseStarted", createTestCaseStartedEvent());
    CommonData commonData = Whitebox.getInternalState(testra, "commonData");
    assertThat(commonData.currentScenarioID).isEqualTo("idStringForScenario");
    assertThat(commonData.currentFeatureID).isEqualTo("idStringForFeature");
  }

  public void createManualResult() throws Exception {
    prepTestraRestClient();
    testra = mock(Testra.class);
    CommonData commonData = createCommonData();
    commonData.currentScenarioID = "idStringForScenario";
    commonData.currentFeatureID = "idStringForFeature";
    commonData.isManual = true;
    Whitebox.setInternalState(testra, "commonData", commonData);
    Whitebox.invokeMethod(testra, "handleTestCaseFinished", mock(TestCaseFinished.class));
    commonData = Whitebox.getInternalState(testra, "commonData");
    assertThat(commonData.resultID).isEqualTo("idStringForResult");
  }

  public void createResult() throws Exception {
    prepTestraRestClient();
    testra = mock(Testra.class);
    CommonData commonData = createCommonData();
    commonData.currentScenarioID = "idStringForScenario";
    commonData.currentFeatureID = "idStringForFeature";
    Whitebox.setInternalState(testra, "commonData", commonData);
    TestCaseFinished testCaseFinished = mock(TestCaseFinished.class);
    Result result = new Result(Type.PASSED, 100L, null);
    Whitebox.setInternalState(testCaseFinished, "result", result);
    Whitebox.invokeMethod(testra, "handleTestCaseFinished", testCaseFinished);
    commonData = Whitebox.getInternalState(testra, "commonData");
    assertThat(commonData.resultID).isEqualTo("idStringForResult");
  }


  private void prepTestraRestClient(){
    testra = mock(Testra.class);
    Whitebox.setInternalState(testra, "commonData", createCommonData());
    Whitebox.setInternalState(TestraRestClient.class, "projectIDString", projectID);
    Whitebox.setInternalState(TestraRestClient.class, "executionIDString", executionID);
  }

  private CommonData createCommonData(){
    CommonData commonData = new CommonData();
    commonData.backgroundSteps = new HashMap<>();
    commonData.cucumberSourceUtils = mock(CucumberSourceUtils.class);
    gherkin.ast.Feature feature = new Feature(Collections.EMPTY_LIST, null,"en", "And", "This is a feature", "Description",Collections.EMPTY_LIST);
    commonData.currentFeature = feature;
    commonData.retryCount = 0;
    commonData.isExpectedFailure = false;
    when(commonData.cucumberSourceUtils.getFeature(any())).thenReturn(feature);
    commonData.backgroundSteps.put(MD5.generateMD5(createTestCaseStartedEvent().testCase.getUri()), Collections.EMPTY_LIST);
    return commonData;
  }

  private TestCaseStarted createTestCaseStartedEvent(){
    List<TestStep> testSteps = new ArrayList<>();
    for(int i = 0; i<3; i++){
      PickleLocation pickleLocation = new PickleLocation(i+5, 11);
      PickleStep pickleStep = new PickleStep(i + "plus 3 equals " + (3+i),  Collections.EMPTY_LIST, Collections.singletonList(pickleLocation));
      StepDefinition stepDefinition = new JavaStepDefinition();
      DefinitionMatch definitionMatch = new StepDefinitionMatch(null,stepDefinition,null,null,null);
      TestStep testStep = new PickleTestStep("/src/test/resources/feature_files/feature.feature",
          pickleStep, definitionMatch);
      testSteps.add(testStep);
    }
    Pickle pickle = new Pickle("My scenario", "en", Collections.EMPTY_LIST, new ArrayList<>(), Collections.singletonList(new PickleLocation(5,11)));
    PickleEvent pickleEvent = new PickleEvent("pickle",pickle);
    TestCase testCase = new TestCase(testSteps,pickleEvent, false);
    return new TestCaseStarted(0L, testCase);
  }


}
