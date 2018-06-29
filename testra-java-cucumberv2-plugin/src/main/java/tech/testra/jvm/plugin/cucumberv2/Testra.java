package tech.testra.jvm.plugin.cucumberv2;

import cucumber.api.Result;
import cucumber.api.Result.Type;
import cucumber.api.event.*;
import cucumber.api.formatter.Formatter;
import gherkin.ast.Feature;
import gherkin.ast.ScenarioDefinition;
import gherkin.ast.Step;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import tech.testra.java.client.TestraRestClient;
import tech.testra.java.client.model.*;
import tech.testra.java.client.model.TestResultRequest.ResultEnum;
import tech.testra.java.client.model.TestResultRequest.ResultTypeEnum;
import tech.testra.jvm.commons.util.PropertyHelper;
import tech.testra.jvm.plugin.cucumberv2.utils.CommonData;
import tech.testra.jvm.plugin.cucumberv2.utils.CommonDataProvider;
import tech.testra.jvm.plugin.cucumberv2.utils.CucumberSourceUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static tech.testra.jvm.commons.util.PropertyHelper.prop;


public class Testra implements Formatter {

  private static final Logger LOGGER = LoggerFactory.getLogger(Testra.class);

  private final Long threadId = Thread.currentThread().getId();
  private static String projectID;
  private CommonData commonData;
  private final EventHandler<TestSourceRead> featureStartedHandler = this::handleFeatureStartedHandler;
  private final EventHandler<TestCaseStarted> caseStartedHandler = this::handleTestCaseStarted;
  private final EventHandler<TestCaseFinished> caseFinishedHandler = this::handleTestCaseFinished;
  private final EventHandler<TestStepStarted> stepStartedHandler = this::handleTestStepStarted;
  private final EventHandler<TestStepFinished> stepFinishedHandler = this::handleTestStepFinished;
  private final EventHandler<SnippetsSuggestedEvent> snippetsSuggestedEventEventHandler = this::snippetHandler;
  private final EventHandler<TestRunFinished> runFinishedEventHandler = this::handleRunFinishedEventHandler;

  private void handleRunFinishedEventHandler(TestRunFinished testRunFinished) {
    List<TestResult> results = TestraRestClient.getResults(null);
    int passed = 0;
    int failed = 0;
    int skipped = 0;
    int undefined = 0;
    for(TestResult testResult : results){
      switch(testResult.getResult()){
        case PASSED:
          passed++;
          break;
        case SKIPPED:
          skipped++;
          break;
        case FAILED:
          failed++;
          break;
        case UNDEFINED:
          undefined++;
          break;
      }
    }
    LOGGER.info("RESULTS:");
    LOGGER.info("PASSED:" + passed);
    LOGGER.info("SKIPPED:" + skipped);
    LOGGER.info("FAILED:" + failed);
    LOGGER.info("UNDEFINED: " + undefined);
  }

  private final CucumberSourceUtils cucumberSourceUtils = new CucumberSourceUtils();
  private EventHandler<EmbedEvent> embedEventhandler = event -> handleEmbed(event);

  public Testra(String properties) {
    File propertyFile = new File(properties);
    if(propertyFile.isFile()){
      PropertyHelper.loadPropertiesFromAbsolute(propertyFile.getAbsolutePath());
      LOGGER.info("Loaded properties from filepath " + propertyFile.getAbsolutePath());
    }
    else{
      LOGGER.info("Property file not found at " + propertyFile.getAbsolutePath() + " using default");
      PropertyHelper.loadPropertiesFromAbsolute(".testra");
    }
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

  public Testra() {
    PropertyHelper.loadPropertiesFromAbsolute(".testra");
    setup();
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
    publisher.registerHandlerFor(EmbedEvent.class, embedEventhandler);
    publisher.registerHandlerFor(SnippetsSuggestedEvent.class, snippetsSuggestedEventEventHandler);
    publisher.registerHandlerFor(TestRunFinished.class, runFinishedEventHandler);
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

  private void snippetHandler(SnippetsSuggestedEvent event){
    commonData.snippetLine.add(event.snippets.get(0));
  }

  private void handleFeatureStartedHandler(final TestSourceRead event) {

    cucumberSourceUtils.addTestSourceReadEvent(event.uri,event);
    commonData.cucumberSourceUtils.addTestSourceReadEvent(event.uri,event);
    cucumberSourceUtils.getFeature(event.uri);
    processBackgroundSteps(commonData.cucumberSourceUtils.getFeature(event.uri));
    if(commonData.isRetry) {
      List<TestResult> failedTests = TestraRestClient.getFailedResults();
      failedTests.forEach(x -> {commonData.failedScenarioIDs.put(x.getTargetId(),x.getId());
      commonData.failedRetryMap.put(x.getTargetId(), x.getRetryCount());});
    }
  }
  private synchronized void createExecution() {
      if(TestraRestClient.getExecutionid() == null) {
        if(commonData.isRetry){
          TestraRestClient.setExecutionid(prop("previousexecutionID"));
        }
        else if(commonData.setExecutionID){
          TestraRestClient.setExecutionid(prop("previousexecutionID"));
        }
        else {
          TestraRestClient.createExecution();
        }
      }
  }

  private void handleEmbed(EmbedEvent event) {
    commonData.embedEvent = event;
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
    commonData.startTime = System.currentTimeMillis();
    commonData.currentFeatureFile = event.testCase.getUri();
    commonData.currentFeature = commonData.cucumberSourceUtils.getFeature(commonData.currentFeatureFile);
    commonData.currentTestCase = event.testCase;
    final List<String> tagList = new ArrayList<>();
    event.testCase.getTags().forEach(x -> tagList.add(x.getName()));
    ScenarioRequest scenarioRequest = new tech.testra.java.client.model.ScenarioRequest();
    scenarioRequest.setName(event.testCase.getName());
    scenarioRequest.setFeatureName(commonData.currentFeature.getName());
    scenarioRequest.setFeatureDescription(commonData.currentFeature.getDescription());
    scenarioRequest.setTags(tagList);
    scenarioRequest.setBackgroundSteps(commonData.backgroundSteps.stream()
        .map(s -> {
          TestStep testStep = new TestStep();
          testStep.setIndex(s.getIndex());
        testStep.setText(s.getGherkinStep());
        return testStep;}).collect(Collectors.toList()));
    List<TestStep> testStepList = new ArrayList<>();
    for(int i = commonData.backgroundSteps.size(); i<commonData.currentTestCase.getTestSteps().size(); i++){
      if(!(commonData.currentTestCase.getTestSteps().get(i).isHook())) {
        TestStep testStep = new TestStep();
        testStep.setIndex(i - commonData.backgroundSteps.size());
        testStep.setText(((commonData.currentTestCase.getTestSteps().get(i))).getStepText());
        testStepList.add(testStep);

      }
    }
    scenarioRequest.setSteps(testStepList);
    Scenario scenario = TestraRestClient.createScenario(scenarioRequest);
    commonData.currentScenarioID = scenario.getId();
    commonData.currentFeatureID = scenario.getFeatureId();

    if(commonData.isRetry&&!commonData.failedScenarioIDs.containsKey(commonData.currentScenarioID)){
      LOGGER.info("Test has already passed in a previous test run");
      throw new SkipException("Test passed in previous test run, skipping");
    }

    if(commonData.isRetry){
      commonData.currentTestResultID = commonData.failedScenarioIDs.get(commonData.currentScenarioID);
      commonData.retryCount = commonData.failedRetryMap.get(commonData.currentScenarioID) + 1;
    }

  }

  static TestResultRequest.ResultEnum TRtoTRR(TestResult.ResultEnum value) {
    return TestResultRequest.ResultEnum.values()[value.ordinal()];
  }

  private void handleTestStepStarted(final TestStepStarted event) {
  }

  private void handleTestStepFinished(final TestStepFinished event) {
    if (event.testStep.isHook()) {
      handleHookStep(event);
    } else {
      handlePickleStep(event);
    }
  }

  private void handleHookStep(final TestStepFinished event) {
  }

  private void handleTestCaseFinished(final TestCaseFinished event) {
    commonData.endTime = System.currentTimeMillis();
    TestResultRequest testResultRequest = new TestResultRequest();
    testResultRequest.setGroupId(commonData.currentFeatureID);
    testResultRequest.setTargetId(commonData.currentScenarioID);
    testResultRequest.setDurationInMs(event.result.getDuration());
    testResultRequest.setResultType(ResultTypeEnum.SCENARIO);
    testResultRequest.setResult(resultToEnum(event.result.getStatus()));
    testResultRequest.setStepResults(commonData.stepResultsNew);
    testResultRequest.setStartTime(commonData.startTime);
    testResultRequest.setEndTime(commonData.endTime);
    testResultRequest.setRetryCount(commonData.retryCount);
    if(event.result.getStatus().equals(Type.FAILED)){
      testResultRequest.setError(event.result.getErrorMessage());
      if(commonData.embedEvent != null){
        Attachment attachment = new Attachment();
        attachment.setMimeType(commonData.embedEvent.mimeType);
        attachment.setBase64EncodedByteArray(new String(Base64.getEncoder().encode(commonData.embedEvent.data)));
        testResultRequest
            .setAttachments(Collections.singletonList(attachment));
      }
    }

    commonData.embedEvent = null;
    if(commonData.isRetry){
      TestraRestClient.updateResult(commonData.currentTestResultID,testResultRequest);
    }
    else
      TestraRestClient.createResult(testResultRequest);
    commonData.stepResultsNew = new ArrayList<>();
  }

  private ResultEnum resultToEnum(Result.Type result){
    switch(result){
      case FAILED:
        return ResultEnum.FAILED;
      case PASSED:
        return ResultEnum.PASSED;
      case PENDING:
        return ResultEnum.PENDING;
      case SKIPPED:
        return ResultEnum.SKIPPED;
      case AMBIGUOUS:
        return ResultEnum.AMBIGUOUS;
      case UNDEFINED:
        return ResultEnum.UNDEFINED;
      default:
        throw new IllegalArgumentException("Result type " + result.toString() + " not found");
    }
  }

  private StepResult.ResultEnum StepResultToEnum(Result.Type result){
    switch(result){
      case FAILED:
        return StepResult.ResultEnum.FAILED;
      case PASSED:
        return StepResult.ResultEnum.PASSED;
      case PENDING:
        return StepResult.ResultEnum.PENDING;
      case SKIPPED:
        return StepResult.ResultEnum.SKIPPED;
      case AMBIGUOUS:
        return StepResult.ResultEnum.AMBIGUOUS;
      case UNDEFINED:
        return StepResult.ResultEnum.UNDEFINED;
      default:
        throw new IllegalArgumentException("Result type " + result.toString() + " not found");
    }
  }

  private void handlePickleStep(final TestStepFinished event) {
    cucumber.api.TestStep testStep = event.testStep;
    StepResult stepResult = new StepResult();
    stepResult.setDurationInMs(event.result.getDuration());
    stepResult.setIndex(testStep.getStepLine());
    stepResult.setResult(StepResultToEnum(event.result.getStatus()));
    if(event.result.getStatus().equals(Type.UNDEFINED)){
      if(commonData.snippetLine.size()>0){
        stepResult.setError(commonData.snippetLine.get(commonData.snippetCount));
        commonData.snippetCount++;
      }
      else {
        stepResult.setError(
            "No step found for scenario line: " + testStep.getPickleStep().getText());
      }
    }
    if(event.result.getStatus().equals(Type.FAILED)){

      stepResult.setError(event.result.getErrorMessage());
    }
    commonData.stepResultsNew.add(stepResult);
  }
}