package com.williamhill.whgtf.testra.jvm.pluginv2;

import static com.williamhill.whgtf.testra.jvm.util.HttpAssertHelper.assertHttpStatusAsOk;
import static com.williamhill.whgtf.testra.jvm.util.PropertyHelper.getEnv;
import static com.williamhill.whgtf.testra.jvm.util.PropertyHelper.prop;

import com.williamhill.whgtf.test.bnw.pojo.executions.ExecutionResponse;
import com.williamhill.whgtf.test.bnw.pojo.scenarios.BackgroundStep;
import com.williamhill.whgtf.test.bnw.pojo.scenarios.ScenarioRequest;
import com.williamhill.whgtf.test.bnw.pojo.scenarios.ScenarioResponse;
import com.williamhill.whgtf.test.bnw.pojo.testresult.Attachment;
import com.williamhill.whgtf.test.bnw.pojo.testresult.TestResultRequest;
import com.williamhill.whgtf.testra.jvm.client.api.TestraRestClient;
import com.williamhill.whgtf.testra.jvm.message.http.HttpResponseMessage;
import com.williamhill.whgtf.testra.jvm.util.JsonObject;
import com.williamhill.whgtf.testra.jvm.util.PropertyHelper;
import cucumber.api.Result.Type;
import cucumber.api.TestCase;
import cucumber.api.event.EventHandler;
import cucumber.api.event.EventPublisher;
import cucumber.api.event.TestCaseFinished;
import cucumber.api.event.TestCaseStarted;
import cucumber.api.event.TestSourceRead;
import cucumber.api.event.TestStepFinished;
import cucumber.api.event.TestStepStarted;
import cucumber.api.formatter.Formatter;
import cucumber.runner.UnskipableStep;
import gherkin.ast.Feature;
import gherkin.ast.ScenarioDefinition;
import gherkin.ast.Step;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestraCucumberJvmV2 implements Formatter {

  static {
    ClassLoader classLoader = TestraCucumberJvmV2.class.getClassLoader();
    PropertyHelper.loadProperties(getEnv() + ".environment.properties", classLoader);
    PropertyHelper.loadProperties("endpoints.properties", classLoader);
  }
  private final CucumberSourceUtils cucumberSourceUtils = new CucumberSourceUtils();
  private Feature currentFeature;
  private String currentFeatureFile;
  private TestCase currentTestCase;
  private final Map<String, String> scenarioUuids = new HashMap<>();
  private static final Logger LOGGER = LoggerFactory.getLogger(TestraCucumberJvmV2.class);
  private final EventHandler<TestSourceRead> featureStartedHandler = this::handleFeatureStartedHandler;
  private final EventHandler<TestCaseStarted> caseStartedHandler = this::handleTestCaseStarted;
  private final EventHandler<TestCaseFinished> caseFinishedHandler = this::handleTestCaseFinished;
  private final EventHandler<TestStepStarted> stepStartedHandler = this::handleTestStepStarted;˚
  private final EventHandler<TestStepFinished> stepFinishedHandler = this::handleTestStepFinished;
  private static TestraRestClient testraRestClient;
  private static String projectID;
  private List<StepTemplate> backgroundSteps = new ArrayList<>();
  private Map<String, String> uuIDToID = new HashMap<>();
  private List<com.williamhill.whgtf.test.bnw.pojo.testresult.StepResult> stepResults = new ArrayList<>();
  private final String TYPE_SCENARIO = "SCENARIO";
  private String currentScenarioID = "";
  private String ExecutionID;
  private static byte[] screenShot;
  private static boolean isScreenshot = false;

  public TestraCucumberJvmV2(){
    projectID = getTestraRestClient().getProjectID(prop("project"));
    LOGGER.info("Project ID is " + projectID);
  }

  public TestraRestClient getTestraRestClient() {
    if (testraRestClient == null) {
      testraRestClient = new TestraRestClient();
    }
    return testraRestClient;
  }

  public static void setScreenShot(byte[] screenshot){
    screenShot = screenshot;
    isScreenshot = true;
  }

  @Override
  public void setEventPublisher(final EventPublisher publisher) {
    publisher.registerHandlerFor(TestSourceRead.class, featureStartedHandler);

    publisher.registerHandlerFor(TestCaseStarted.class, caseStartedHandler);
    publisher.registerHandlerFor(TestCaseFinished.class, caseFinishedHandler);

    publisher.registerHandlerFor(TestStepStarted.class, stepStartedHandler);
    publisher.registerHandlerFor(TestStepFinished.class, stepFinishedHandler);
  }


  private void handleFeatureStartedHandler(final TestSourceRead event) {
    cucumberSourceUtils.addTestSourceReadEvent(event.uri, event);
    createExecution();
    processBackgroundSteps(cucumberSourceUtils.getFeature(event.uri));
  }
  private void createExecution() {
    if (ExecutionID == null) {
      HttpResponseMessage httpResponseMessage = testraRestClient.createExecution();
      assertHttpStatusAsOk(httpResponseMessage);
      ExecutionID = JsonObject
          .jsonToObject(httpResponseMessage.getPayload(), ExecutionResponse.class)
          .getId();
    }
  }

  private void processBackgroundSteps(Feature feature)
  {
    List<String> featureTags = new ArrayList<>();
    feature.getTags().forEach(x -> featureTags.add(x.getName()));
    ScenarioDefinition background = feature.getChildren().stream()
      .filter(s -> s.getClass().getSimpleName().equals("Background"))
        .findFirst()
        .orElse(null);

    if(background!=null){
      int counter = 0;
      for(Step step : background.getSteps()){
        StepTemplate stepTemplate = new StepTemplate();
        stepTemplate.setGherkinStep(step.getKeyword() + step.getText());
        stepTemplate.setIndex(counter);
        stepTemplate.setLine(step.getLocation().getLine());
        counter ++;
        backgroundSteps.add(stepTemplate);
      }
    }
  }



  private void handleTestCaseStarted(final TestCaseStarted event) {
    currentFeatureFile = event.testCase.getUri();
    currentFeature = cucumberSourceUtils.getFeature(currentFeatureFile);

    currentTestCase = event.testCase;

    final List<String> tagList = new ArrayList<>();
    event.testCase.getTags().forEach(x -> tagList.add(x.getName()));
    ScenarioRequest scenarioRequest = new ScenarioRequest();
    scenarioRequest.setTags(tagList);
    scenarioRequest.setFeatureDescription(currentFeature.getDescription());
    scenarioRequest.setName(event.testCase.getName());
    scenarioRequest.setBackgroundSteps(backgroundSteps.stream()
        .map(s -> new BackgroundStep().withIndex(s.getIndex())
            .withText(s.getGherkinStep())).collect(Collectors.toList()));

    List<com.williamhill.whgtf.test.bnw.pojo.scenarios.Step> testSteps = new ArrayList<>();
    for(int i = backgroundSteps.size(); i<currentTestCase.getTestSteps().size(); i++){
      if(!currentTestCase.getTestSteps().get(i).isHook()) {
        testSteps.add(new com.williamhill.whgtf.test.bnw.pojo.scenarios.Step()
            .withIndex(i - backgroundSteps.size())
            .withText(currentTestCase.getTestSteps().get(i).getStepText()));
      }
    }
    scenarioRequest.setSteps(testSteps);

    currentScenarioID = JsonObject
        .jsonToObject(testraRestClient.addScenario(scenarioRequest).getPayload(),
            ScenarioResponse.class).getId();
  }

  private void handleTestStepStarted(final TestStepStarted event) {
  }

  private void handleTestStepFinished(final TestStepFinished event) {
    if (event.testStep.isHook() && event.testStep instanceof UnskipableStep) {
      handleHookStep(event);
    } else {
      handlePickleStep(event);
    }
  }

  private void handleHookStep(final TestStepFinished event) {
  }

  private void handleTestCaseFinished(final TestCaseFinished event) {

    TestResultRequest testResultRequest = new TestResultRequest()
        .withTargetId(currentScenarioID)
        .withDurationInMs(event.result.getDuration())
        .withResultType(TYPE_SCENARIO)
        .withResult(event.result.getStatus().toString())
        .withStepResults(stepResults)
        .withStartTime(1)
        .withEndTime(1);

    if(event.result.getStatus().equals(Type.FAILED)){
      testResultRequest.setError(event.result.getErrorMessage());
      if(isScreenshot){
        testResultRequest
            .setAttachments(Collections.singletonList(new Attachment()
                .withName("Failure Screenshot")
                .withBase64EncodedByteArray(new String(Base64.getEncoder().encode(screenShot)))));
      }
    }
    screenShot = null;
    testraRestClient.addTestResult(testResultRequest, ExecutionID);
    stepResults = new ArrayList<>();
  }

  private void handlePickleStep(final TestStepFinished event) {
    com.williamhill.whgtf.test.bnw.pojo.testresult.StepResult stepResult = new com.williamhill.whgtf.test.bnw.pojo.testresult.StepResult();
    stepResult.setDurationInMs(event.result.getDuration());
    stepResult.setIndex(event.testStep.getStepLine());
    stepResult.setResult(event.result.getStatus().toString());
    if(event.result.getStatus().equals(Type.FAILED)){
      stepResult.setError(event.result.getErrorMessage());
    }
    stepResults.add(stepResult);
  }
}
