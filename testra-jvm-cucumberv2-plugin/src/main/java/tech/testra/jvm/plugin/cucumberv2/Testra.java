package tech.testra.jvm.plugin.cucumberv2;

import cucumber.api.Result.Type;
import cucumber.api.event.*;
import cucumber.api.formatter.Formatter;
import cucumber.runner.UnskipableStep;
import gherkin.ast.Feature;
import gherkin.ast.ScenarioDefinition;
import gherkin.ast.Step;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.testra.jvm.api.client.models.executions.ExecutionResponse;
import tech.testra.jvm.api.client.models.scenarios.BackgroundStep;
import tech.testra.jvm.api.client.models.scenarios.ScenarioRequest;
import tech.testra.jvm.api.client.models.scenarios.ScenarioResponse;
import tech.testra.jvm.api.client.models.testresult.Attachment;
import tech.testra.jvm.api.client.models.testresult.TestResultRequest;
import tech.testra.jvm.api.message.http.HttpResponseMessage;
import tech.testra.jvm.api.util.JsonObject;
import tech.testra.jvm.api.util.PropertyHelper;
import tech.testra.jvm.plugin.cucumberv2.utils.CommonData;
import tech.testra.jvm.plugin.cucumberv2.utils.CommonDataProvider;
import tech.testra.jvm.plugin.cucumberv2.utils.CucumberSourceUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static tech.testra.jvm.api.util.HttpAssertHelper.assertHttpStatusAsOk;
import static tech.testra.jvm.api.util.PropertyHelper.getEnv;
import static tech.testra.jvm.api.util.PropertyHelper.prop;

public class Testra implements Formatter {

  private static final Logger LOGGER = LoggerFactory.getLogger(Testra.class);

  static {
    ClassLoader classLoader = Testra.class.getClassLoader();
    PropertyHelper.loadProperties("endpoints.properties", classLoader);
  }
  private final Long threadId = Thread.currentThread().getId();
  private static String projectID;
  private static String executionID;
  private CommonData commonData;
  private final EventHandler<TestSourceRead> featureStartedHandler = this::handleFeatureStartedHandler;
  private final EventHandler<TestCaseStarted> caseStartedHandler = this::handleTestCaseStarted;
  private final EventHandler<TestCaseFinished> caseFinishedHandler = this::handleTestCaseFinished;
  private final EventHandler<TestStepStarted> stepStartedHandler = this::handleTestStepStarted;
  private final EventHandler<TestStepFinished> stepFinishedHandler = this::handleTestStepFinished;
  private final CucumberSourceUtils cucumberSourceUtils = new CucumberSourceUtils();

  public Testra(String properties) {
    File propertyFile = new File(properties);
    if(propertyFile.isFile()){
      PropertyHelper.loadPropertiesFromAbsolute(propertyFile.getAbsolutePath());
      LOGGER.info("Loaded properties from filepath " + propertyFile.getAbsolutePath());
    }
    else{
      LOGGER.info("Property file not found at " + propertyFile.getAbsolutePath() + " using default");
      PropertyHelper.loadProperties(getEnv() + ".environment.properties", Testra.class.getClassLoader());
    }

    setup();
    projectID = commonData.getTestraRestClient().getProjectID(prop("project"));
    LOGGER.info("Project ID is " + projectID);
    createExecution();
  }

  public Testra() {
    PropertyHelper.loadProperties(getEnv() + ".environment.properties", Testra.class.getClassLoader());
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

    List<tech.testra.jvm.api.client.models.scenarios.Step> testSteps = new ArrayList<>();
    for(int i = commonData.backgroundSteps.size(); i<commonData.currentTestCase.getTestSteps().size(); i++){
      if(!commonData.currentTestCase.getTestSteps().get(i).isHook()) {
        testSteps.add(new tech.testra.jvm.api.client.models.scenarios.Step()
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

    String TYPE_SCENARIO = "SCENARIO";
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
        try {
          testResultRequest
              .setAttachments(Collections.singletonList(new Attachment()
                  .withName("Failure Screenshot")
                  .withBase64EncodedByteArray(
                      new String(Base64.getEncoder().encode(commonData.screenShot)))));
        }
        catch(NullPointerException e){
          LOGGER.error("Error no screenshot found, have you implemented the screenshot in @After?");
        }
      }
    }
    commonData.setScreenShot(null);
    commonData.getTestraRestClient().addTestResult(testResultRequest, executionID);
    commonData.stepResults = new ArrayList<>();
  }

  public static void setScreenshot(byte[] screenshot){
    CommonDataProvider.get(Thread.currentThread().getId()).setScreenShot(screenshot);
  }

  private void handlePickleStep(final TestStepFinished event) {
    tech.testra.jvm.api.client.models.testresult.StepResult stepResult = new tech.testra.jvm.api.client.models.testresult.StepResult();
    stepResult.setDurationInMs(event.result.getDuration());
    stepResult.setIndex(event.testStep.getStepLine());
    stepResult.setResult(event.result.getStatus().toString());
    if(event.result.getStatus().equals(Type.FAILED)){
      stepResult.setError(event.result.getErrorMessage());
    }
    commonData.stepResults.add(stepResult);
  }
}
