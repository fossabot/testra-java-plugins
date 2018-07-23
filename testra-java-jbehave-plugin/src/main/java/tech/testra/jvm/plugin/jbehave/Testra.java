package tech.testra.jvm.plugin.jbehave;

import static tech.testra.jvm.commons.util.PropertyHelper.prop;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jbehave.core.model.Scenario;
import org.jbehave.core.model.Story;
import org.jbehave.core.reporters.NullStoryReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.testra.java.client.TestraRestClient;
import tech.testra.java.client.model.EnrichedTestResult;
import tech.testra.java.client.model.ScenarioRequest;
import tech.testra.java.client.model.StepResult;
import tech.testra.java.client.model.StepResult.StatusEnum;
import tech.testra.java.client.model.TestResultRequest;
import tech.testra.java.client.model.TestResultRequest.ResultTypeEnum;
import tech.testra.java.client.model.TestStep;
import tech.testra.jvm.commons.util.PropertyHelper;
import tech.testra.jvm.plugin.jbehave.utils.CommonData;
import tech.testra.jvm.plugin.jbehave.utils.CommonDataProvider;

public class Testra extends NullStoryReporter {

  private static final Logger LOGGER = LoggerFactory.getLogger(Testra.class);
  private final Long threadId = Thread.currentThread().getId();
  private CommonData commonData;
  private String projectID;
  private String executionID;
  private final ThreadLocal<Story> stories = new InheritableThreadLocal<>();
  private final ThreadLocal<String> scenarios
      = InheritableThreadLocal.withInitial(() -> UUID.randomUUID().toString());

  public Testra() {
    PropertyHelper.loadPropertiesFromAbsolute(new File(".testra").getAbsolutePath());
    setup();
    setProperties();
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

  private void setProperties(){
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
            Scanner fileReader = new Scanner(file);
            TestraRestClient.setExecutionid(fileReader.nextLine());
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
        TestraRestClient.createExecutionIDFile();
      }
    }
    executionID = TestraRestClient.getExecutionid();
    LOGGER.info("Execution ID is: " + executionID);
    LOGGER.info(prop("host") + "/projects/" + projectID + "/executions/"+ executionID+"\n");
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

  @Override
  public void beforeStory(final Story story, final boolean givenStory) {
    stories.set(story);
  }

  @Override
  public void afterStory(final boolean givenStory) {
    stories.remove();
  }

  @Override
  public void beforeScenario(final String title) {
    commonData.startTime = System.currentTimeMillis();
    final Story story = stories.get();
    final String uuid = scenarios.get();
    final String fullName = String.format("%s: %s", story.getName(), title);
    commonData.currentFeatureFile = story.getName();
    Scenario scenario = story.getScenarios().stream().filter(x -> x.getTitle().equals(title)).collect(Collectors
        .toList()).get(0);
    List<TestStep> steps = new ArrayList<>();

    for(int i = 0; i< scenario.getSteps().size(); i++){
      TestStep testStep = new TestStep();
      testStep.setIndex(i);
      testStep.setText(scenario.getSteps().get(i));
      steps.add(testStep);
    }
    ScenarioRequest scenarioRequest = new ScenarioRequest();
    scenarioRequest.setFeatureName(commonData.currentFeatureFile);
    scenarioRequest.setFeatureDescription("");
    scenarioRequest.setName(title);
    scenarioRequest.setTags(Collections.emptyList());
    scenarioRequest.setSteps(steps);
    if(scenario.getMeta().getPropertyNames().contains("Manual")){
      scenarioRequest.setManual(true);
      commonData.isManual = true;
    }
    else{
      commonData.isManual = false;
    }
    if(scenario.getMeta().getPropertyNames().contains("ExpectedFailure")){
      commonData.isExpectedFailure = true;
    }
    else{
      commonData.isExpectedFailure = false;
    }
    List<String> tagList = new ArrayList<>(story.getMeta().getPropertyNames());
    scenario.getMeta().getPropertyNames().forEach(tagList::add);
    scenarioRequest.setTags(tagList);
    tech.testra.java.client.model.Scenario scenarioResponse = TestraRestClient.createScenario(scenarioRequest);
    commonData.currentScenarioID = scenarioResponse.getId();
    commonData.currentFeatureID = scenarioResponse.getFeatureId();
    if(commonData.isRetry&&!commonData.failedScenarioIDs.containsKey(commonData.currentScenarioID)){
      LOGGER.info("Test has already passed in a previous test run");
      if(prop("junit") ==null || !Boolean.parseBoolean(prop("junit"))){
        //TODO - for testng
        commonData.skip = true;
      }
      else{
        commonData.skip = true;
      }
    }
    else if(commonData.isRetry){
      commonData.currentTestResultID = commonData.failedScenarioIDs.get(commonData.currentScenarioID);
      commonData.retryCount = commonData.failedRetryMap.get(commonData.currentScenarioID) + 1;
    }
  }

  @Override
  public void afterScenario() {
    if(commonData.skip){
      commonData.skip = false;
      commonData.stepResultsNew = new ArrayList<>();
      return;
    }
    if(commonData.isManual){
      TestResultRequest testResultRequest = new TestResultRequest();
      testResultRequest.setGroupId(commonData.currentFeatureID);
      testResultRequest.setTargetId(commonData.currentScenarioID);
      testResultRequest.setDurationInMs(0L);
      testResultRequest.setResultType(ResultTypeEnum.SCENARIO);
      testResultRequest.setStatus(TestResultRequest.StatusEnum.SKIPPED);
      testResultRequest.setStepResults(null);
      testResultRequest.setStartTime(0L);
      testResultRequest.setEndTime(0L);
      testResultRequest.setRetryCount(commonData.retryCount);
      testResultRequest.setExpectedToFail(commonData.isExpectedFailure);
      commonData.stepResultsNew = new ArrayList<>();
      commonData.resultID = TestraRestClient.createResult(testResultRequest).getId();
      return;
    }
    commonData.resultCounter = 0;
    commonData.endTime = System.currentTimeMillis();
    TestResultRequest testResultRequest = new TestResultRequest();
    testResultRequest.setGroupId(commonData.currentFeatureID);
    testResultRequest.setTargetId(commonData.currentScenarioID);
    testResultRequest.setDurationInMs(System.currentTimeMillis()-commonData.startTime);
    testResultRequest.setResultType(ResultTypeEnum.SCENARIO);
    TestResultRequest.StatusEnum status;
    if(commonData.isFailed){
      status = TestResultRequest.StatusEnum.FAILED;
    }
    else if(commonData.isSkipped){
      status = TestResultRequest.StatusEnum.SKIPPED;
    }
    else if(commonData.isPending){
      status = TestResultRequest.StatusEnum.PENDING;
    }
    else{
      status = TestResultRequest.StatusEnum.PASSED;
    }
    commonData.isPending = false;
    commonData.isSkipped = false;
    testResultRequest.setStatus(status);
    testResultRequest.setStepResults(commonData.stepResultsNew);
    testResultRequest.setStartTime(commonData.startTime);
    testResultRequest.setEndTime(commonData.endTime);
    testResultRequest.setRetryCount(commonData.retryCount);
    testResultRequest.setExpectedToFail(commonData.isExpectedFailure);
    commonData.isExpectedFailure = false;
    if(commonData.isFailed){
      commonData.isFailed = false;
      testResultRequest.setError(commonData.failureMessage);
    }
    commonData.stepResultsNew = new ArrayList<>();
    if(commonData.isRetry){
      commonData.resultID = TestraRestClient.updateResult(commonData.currentTestResultID,testResultRequest).getId();
    }
    else
      commonData.resultID = TestraRestClient.createResult(testResultRequest).getId();
  }

  @Override
  public void beforeStep(final String step) {
    commonData.stepStartTime = System.currentTimeMillis();
  }

  @Override
  public void successful(final String step) {
    StepResult stepResult = new StepResult();
    stepResult.setDurationInMs(System.currentTimeMillis()-commonData.stepStartTime);
    stepResult.setIndex(commonData.resultCounter);
    commonData.resultCounter = commonData.resultCounter + 1;
    stepResult.setStatus(StatusEnum.PASSED);
    commonData.stepResultsNew.add(stepResult);
  }

  @Override
  public void ignorable(final String step) {
    StepResult stepResult = new StepResult();
    stepResult.setDurationInMs(System.currentTimeMillis()-commonData.stepStartTime);
    stepResult.setIndex(commonData.resultCounter);
    commonData.resultCounter = commonData.resultCounter + 1;
    stepResult.setStatus(StatusEnum.SKIPPED);
    commonData.stepResultsNew.add(stepResult);
    commonData.isSkipped = true;
  }

  @Override
  public void pending(final String step) {
    StepResult stepResult = new StepResult();
    stepResult.setDurationInMs(System.currentTimeMillis()-commonData.stepStartTime);
    stepResult.setIndex(commonData.resultCounter);
    commonData.resultCounter = commonData.resultCounter + 1;
    stepResult.setStatus(StatusEnum.PENDING);
    commonData.stepResultsNew.add(stepResult);
    commonData.isPending = true;
  }

  @Override
  public void failed(final String step, final Throwable cause) {
    StepResult stepResult = new StepResult();
    stepResult.setDurationInMs(System.currentTimeMillis()-commonData.stepStartTime);
    stepResult.setIndex(commonData.resultCounter);
    commonData.resultCounter = commonData.resultCounter + 1;
    stepResult.setStatus(StatusEnum.FAILED);
    StringBuilder sb = new StringBuilder();
    for(StackTraceElement e : cause.getCause().getStackTrace()){
      sb.append("\n" + e.toString());
    }
    stepResult.setError(cause.getCause().getLocalizedMessage() + sb);
    commonData.stepResultsNew.add(stepResult);
    commonData.isFailed = true;
    commonData.failureMessage = cause.getCause().getLocalizedMessage() + sb;
  }

}
