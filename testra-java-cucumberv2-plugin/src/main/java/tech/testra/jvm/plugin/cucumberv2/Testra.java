package tech.testra.jvm.plugin.cucumberv2;

import cucumber.api.Result;
import cucumber.api.Result.Type;
import cucumber.api.event.*;
import cucumber.api.formatter.Formatter;
import cucumber.runner.PickleTestStep;
import gherkin.ast.Feature;
import gherkin.ast.ScenarioDefinition;
import gherkin.ast.Step;
import java.io.FileWriter;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.reporters.Files;
import tech.testra.java.client.TestraRestClient;
import tech.testra.java.client.model.*;
import tech.testra.java.client.model.TestResultRequest.ResultEnum;
import tech.testra.java.client.model.TestResultRequest.ResultTypeEnum;
import tech.testra.jvm.commons.util.MD5;
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
  private CommonData commonData;
  private final EventHandler<TestSourceRead> featureStartedHandler;
  private final EventHandler<TestCaseStarted> caseStartedHandler;
  private final EventHandler<TestCaseFinished> caseFinishedHandler;
  private final EventHandler<TestStepStarted> stepStartedHandler;
  private final EventHandler<TestStepFinished> stepFinishedHandler;
  private final EventHandler<SnippetsSuggestedEvent> snippetsSuggestedEventEventHandler;

  private final CucumberSourceUtils cucumberSourceUtils = new CucumberSourceUtils();
  private EventHandler<EmbedEvent> embedEventhandler = event -> handleEmbed(event);

  public Testra() {
    PropertyHelper.loadPropertiesFromAbsolute(new File(".testra").getAbsolutePath());
    setup();
    if(Boolean.parseBoolean(prop("testra.disabled" ,"false"))){
      featureStartedHandler = this::handleFeatureStartedHandlerDisabled;
      caseStartedHandler = this::handleTestCaseStartedDisabled;
      caseFinishedHandler = this::handleTestCaseFinishedDisabled;
      stepStartedHandler = this::handleTestStepStartedDisabled;
      stepFinishedHandler = this::handleTestStepFinishedDisabled;
      snippetsSuggestedEventEventHandler = this::snippetHandler;
    }
    else{
      featureStartedHandler = this::handleFeatureStartedHandler;
      caseStartedHandler = this::handleTestCaseStarted;
      caseFinishedHandler = this::handleTestCaseFinished;
      stepStartedHandler = this::handleTestStepStarted;
      stepFinishedHandler = this::handleTestStepFinished;
      snippetsSuggestedEventEventHandler = this::snippetHandler;
      TestraRestClient.setURLs(prop("host"));
      LOGGER.info("Project ID is " + TestraRestClient.getProjectID(prop("project")));
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

  }

  private void handleFeatureStartedHandlerDisabled(final TestSourceRead event) {

  }

  private void handleTestCaseStartedDisabled(TestCaseStarted event){

  }

  private void handleTestStepStartedDisabled(final TestStepStarted event) {
  }

  private void handleTestStepFinishedDisabled(final TestStepFinished event) {
  }

  private void handleTestCaseFinishedDisabled(final TestCaseFinished event) {

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
    processBackgroundSteps(commonData.cucumberSourceUtils.getFeature(event.uri), MD5.generateMD5(event.uri));
    if(commonData.isRetry) {
      List<EnrichedTestResult> failedTests = TestraRestClient.getFailedResults();
      failedTests.forEach(x -> {commonData.failedScenarioIDs.put(x.getTargetId(),x.getId());
      commonData.failedRetryMap.put(x.getTargetId(), x.getRetryCount());});
    }
  }
  private synchronized void createExecution() {
      if(TestraRestClient.getExecutionid() == null) {
        if(commonData.isRetry){
          File file = new File("testra.exec");
          if(file.isFile()){
            try {
              TestraRestClient.setExecutionid(Files.readFile(file).trim());
            } catch (IOException e) {
              e.printStackTrace();
            }
          }
          else {
            TestraRestClient.setExecutionid(prop("previousexecutionID"));
          }
        }
        else if(commonData.setExecutionID){
          TestraRestClient.setExecutionid(prop("previousexecutionID"));
        }
        else {
          TestraRestClient.setExecutionid(null);
          createExecutionIDFile();
        }
      }
      LOGGER.info("Execution ID is: " + TestraRestClient.getExecutionid());
  }

  private void createExecutionIDFile(){
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

  private void handleEmbed(EmbedEvent event) {
    commonData.embedEvent = event;
  }

  private void processBackgroundSteps(Feature feature, String featureHash)
  {
    List<StepTemplate> backgroundSteps = new ArrayList<>();
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
    commonData.backgroundSteps.put(featureHash,backgroundSteps);
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
    scenarioRequest.setBackgroundSteps(commonData.backgroundSteps.get(MD5.generateMD5(event.testCase.getUri())).stream()
        .map(s -> {
          TestStep testStep = new TestStep();
          testStep.setIndex(s.getIndex());
        testStep.setText(s.getGherkinStep());
        return testStep;}).collect(Collectors.toList()));
    List<TestStep> testStepList = new ArrayList<>();
    List<PickleTestStep> pickleSteps = commonData.currentTestCase.getTestSteps().stream()
        .filter(x -> !x.isHook())
        .map(x -> (PickleTestStep)x)
        .collect(Collectors.toList());
    for(int i = commonData.backgroundSteps.get(MD5.generateMD5(event.testCase.getUri())).size(); i<pickleSteps.size(); i++){
        TestStep testStep = new TestStep();
        testStep.setIndex(i);
        testStep.setText(((pickleSteps.get(i))).getStepText());
        testStepList.add(testStep);

    }
    scenarioRequest.setSteps(testStepList);
    Scenario scenario = TestraRestClient.createScenario(scenarioRequest);
    commonData.currentScenarioID = scenario.getId();
    commonData.currentFeatureID = scenario.getFeatureId();

    if(commonData.isRetry&&!commonData.failedScenarioIDs.containsKey(commonData.currentScenarioID)){
      LOGGER.info("Test has already passed in a previous test run");
      if(prop("junit") ==null || !Boolean.parseBoolean(prop("junit"))){
          throw new SkipException("Test already passed, skipping");
      }
    }

    else if(commonData.isRetry){
      commonData.currentTestResultID = commonData.failedScenarioIDs.get(commonData.currentScenarioID);
      commonData.retryCount = commonData.failedRetryMap.get(commonData.currentScenarioID) + 1;
    }

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
    if(commonData.skip){
      commonData.skip = false;
      commonData.stepResultsNew = new ArrayList<>();
      commonData.embedEvent = null;
      return;
    }
    commonData.resultCounter = 0;
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
    stepResult.setIndex(commonData.resultCounter);
    commonData.resultCounter = commonData.resultCounter + 1;
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