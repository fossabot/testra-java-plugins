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
import com.williamhill.whgtf.testra.jvm.message.http.HttpResponseMessage;
import com.williamhill.whgtf.testra.jvm.pluginv2.utils.CommonData;
import com.williamhill.whgtf.testra.jvm.pluginv2.utils.CommonDataProvider;
import com.williamhill.whgtf.testra.jvm.pluginv2.utils.CucumberSourceUtils;
import com.williamhill.whgtf.testra.jvm.util.JsonObject;
import com.williamhill.whgtf.testra.jvm.util.PropertyHelper;
import cucumber.api.Result.Type;
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
import java.io.File;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestraCucumberJvmV2 implements Formatter {

  static {
    ClassLoader classLoader = TestraCucumberJvmV2.class.getClassLoader();

    PropertyHelper.loadProperties("endpoints.properties", classLoader);
  }
  private static final Logger LOGGER = LoggerFactory.getLogger(TestraCucumberJvmV2.class);
  private final Long threadId = Thread.currentThread().getId();
  private static String projectID;
  private static String executionID;
  private CommonData commonData;
  private final EventHandler<TestSourceRead> featureStartedHandler = this::handleFeatureStartedHandler;
  private final EventHandler<TestCaseStarted> caseStartedHandler = this::handleTestCaseStarted;
  private final EventHandler<TestCaseFinished> caseFinishedHandler = this::handleTestCaseFinished;
  private final EventHandler<TestStepStarted> stepStartedHandler = this::handleTestStepStarted;
  private final EventHandler<TestStepFinished> stepFinishedHandler = this::handleTestStepFinished;
  private final String TYPE_SCENARIO = "SCENARIO";
  private final CucumberSourceUtils cucumberSourceUtils = new CucumberSourceUtils();

  public TestraCucumberJvmV2(String properties){
    File propertyFile = new File(properties);
    ClassLoader classLoader = TestraCucumberJvmV2.class.getClassLoader();
    if(propertyFile.isFile()){
      PropertyHelper.loadPropertiesFromAbsolute(propertyFile.getAbsolutePath());
      LOGGER.info("loaded properties from filepath " + propertyFile.getAbsolutePath());
    }
    else{
      LOGGER.info("property file not found at " + propertyFile.getAbsolutePath() + " using default");
      PropertyHelper.loadProperties(getEnv() + ".environment.properties", classLoader);
    }
    setup();
    projectID = commonData.getTestraRestClient().getProjectID(prop("project"));
    LOGGER.info("Project ID is " + projectID);
    createExecution();
  }

  @Override
  public void setEventPublisher(final EventPublisher publisher) {
    publisher.registerHandlerFor(TestSourceRead.class, featureStartedHandler);

    publisher.registerHandlerFor(TestCaseStarted.class, caseStartedHandler);
    publisher.registerHandlerFor(TestCaseFinished.class, caseFinishedHandler);

    publisher.registerHandlerFor(TestStepStarted.class, stepStartedHandler);
    publisher.registerHandlerFor(TestStepFinished.class, stepFinishedHandler);
  }

  private void setup(){
    LOGGER.info("Scenario setup - Begin");
      CommonDataProvider.newCommonData(threadId);
    if ((commonData = CommonDataProvider.get(threadId)) == null) {
      throw new RuntimeException("Common data not found for id : " + threadId);
    } else {
      LOGGER.info("Initialised common data instance for scenario");
    }
    LOGGER.info("Scenario setup - End");
  }


  private void handleFeatureStartedHandler(final TestSourceRead event) {

    cucumberSourceUtils.addTestSourceReadEvent(event.uri,event);
    commonData.cucumberSourceUtils.addTestSourceReadEvent(event.uri,event);
    createExecution();
    cucumberSourceUtils.getFeature(event.uri);
    processBackgroundSteps(commonData.cucumberSourceUtils.getFeature(event.uri));
  }
  private void createExecution() {
    if (executionID == null) {
      HttpResponseMessage httpResponseMessage = commonData.getTestraRestClient().createExecution();
      assertHttpStatusAsOk(httpResponseMessage);
      executionID = JsonObject
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
        commonData.backgroundSteps.add(stepTemplate);
      }
    }
  }



  private void handleTestCaseStarted(final TestCaseStarted event) {
    commonData.currentFeatureFile = event.testCase.getUri();
    commonData.currentFeature = commonData.cucumberSourceUtils.getFeature(commonData.currentFeatureFile);
    commonData.currentTestCase = event.testCase;

    final List<String> tagList = new ArrayList<>();
    event.testCase.getTags().forEach(x -> tagList.add(x.getName()));
    ScenarioRequest scenarioRequest = new ScenarioRequest();
    scenarioRequest.setTags(tagList);
    scenarioRequest.setFeatureDescription(commonData.currentFeature.getDescription());
    scenarioRequest.setName(event.testCase.getName());
    scenarioRequest.setBackgroundSteps(commonData.backgroundSteps.stream()
        .map(s -> new BackgroundStep().withIndex(s.getIndex())
            .withText(s.getGherkinStep())).collect(Collectors.toList()));

    List<com.williamhill.whgtf.test.bnw.pojo.scenarios.Step> testSteps = new ArrayList<>();
    for(int i = commonData.backgroundSteps.size(); i<commonData.currentTestCase.getTestSteps().size(); i++){
      if(!commonData.currentTestCase.getTestSteps().get(i).isHook()) {
        testSteps.add(new com.williamhill.whgtf.test.bnw.pojo.scenarios.Step()
            .withIndex(i - commonData.backgroundSteps.size())
            .withText(commonData.currentTestCase.getTestSteps().get(i).getStepText()));
      }
    }
    scenarioRequest.setSteps(testSteps);

    commonData.currentScenarioID = JsonObject
        .jsonToObject(commonData.getTestraRestClient().addScenario(scenarioRequest).getPayload(),
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
        .withTargetId(commonData.currentScenarioID)
        .withDurationInMs(event.result.getDuration())
        .withResultType(TYPE_SCENARIO)
        .withResult(event.result.getStatus().toString())
        .withStepResults(commonData.stepResults)
        .withStartTime(1)
        .withEndTime(1);

    if(event.result.getStatus().equals(Type.FAILED)){
      testResultRequest.setError(event.result.getErrorMessage());
      if(commonData.isScreenshot){
        testResultRequest
            .setAttachments(Collections.singletonList(new Attachment()
                .withName("Failure Screenshot")
                .withBase64EncodedByteArray(new String(Base64.getEncoder().encode(commonData.screenShot)))));
      }
    }
    commonData.setScreenShot(null);
    commonData.getTestraRestClient().addTestResult(testResultRequest, executionID);
    commonData.stepResults = new ArrayList<>();
  }

  private void handlePickleStep(final TestStepFinished event) {
    com.williamhill.whgtf.test.bnw.pojo.testresult.StepResult stepResult = new com.williamhill.whgtf.test.bnw.pojo.testresult.StepResult();
    stepResult.setDurationInMs(event.result.getDuration());
    stepResult.setIndex(event.testStep.getStepLine());
    stepResult.setResult(event.result.getStatus().toString());
    if(event.result.getStatus().equals(Type.FAILED)){
      stepResult.setError(event.result.getErrorMessage());
    }
    commonData.stepResults.add(stepResult);
  }
}
