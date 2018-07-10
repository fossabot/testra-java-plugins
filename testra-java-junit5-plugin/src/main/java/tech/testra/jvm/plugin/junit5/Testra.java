package tech.testra.jvm.plugin.junit5;

import static tech.testra.jvm.commons.util.PropertyHelper.prop;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.testra.java.client.TestraRestClient;
import tech.testra.java.client.model.EnrichedTestResult;
import tech.testra.java.client.model.TestResultRequest;
import tech.testra.java.client.model.TestResultRequest.ResultEnum;
import tech.testra.java.client.model.TestResultRequest.ResultTypeEnum;
import tech.testra.java.client.model.TestcaseRequest;
import tech.testra.jvm.commons.util.PropertyHelper;
import tech.testra.jvm.plugin.junit5.utils.CommonData;
import tech.testra.jvm.plugin.junit5.utils.CommonDataProvider;

public class Testra implements TestExecutionListener {

  private static final Logger LOGGER = LoggerFactory.getLogger(Testra.class);
  private final Long threadId = Thread.currentThread().getId();
  private CommonData commonData;

  public Testra(){
    PropertyHelper.loadPropertiesFromAbsolute(new File(".testra").getAbsolutePath());
    setup();
    if(Boolean.parseBoolean(prop("testra.disabled"))){
      commonData.isDisabled = true;
      return;
    }
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
    LOGGER.info("Execution ID is: " + TestraRestClient.getExecutionid());
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

  @Override
  public void testPlanExecutionStarted(TestPlan testPlan) {
  }

  @Override
  public void testPlanExecutionFinished(TestPlan testPlan) {
  }

  @Override
  public void dynamicTestRegistered(TestIdentifier testIdentifier) {
  }

  @Override
  public void executionSkipped(TestIdentifier testIdentifier, String reason) {
  }

  @Override
  public void executionStarted(TestIdentifier testIdentifier) {
    commonData.startTime = System.currentTimeMillis();
    if(!testIdentifier.isTest()){
      return;
    }
    final Optional<MethodSource> methodSource = testIdentifier.getSource()
        .filter(MethodSource.class::isInstance)
        .map(MethodSource.class::cast);
    TestcaseRequest testcaseRequest = new TestcaseRequest();
    testcaseRequest.setName(testIdentifier.getDisplayName());
    testcaseRequest.setNamespace("");
    methodSource.ifPresent(source -> {
      String namespace = source.getClassName().substring(0,source.getClassName().lastIndexOf("."));
      String classname = source.getClassName().substring(source.getClassName().lastIndexOf(".")+1);
      testcaseRequest.setClassName(classname);
      testcaseRequest.setNamespace(namespace);});
    if(testcaseRequest.getClassName()==null){
      testcaseRequest.setClassName("NOCLASSPROVIDED");
      testcaseRequest.setNamespace("NONAMESPACEPROVIDED");
    }
    commonData.currentTestCaseID = TestraRestClient.createTestcase(testcaseRequest).getId();
  }

  @Override
  public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
    commonData.endTime = System.currentTimeMillis();
    TestResultRequest testResultRequest = new TestResultRequest();
    switch(testExecutionResult.getStatus()){
      case FAILED:
        testResultRequest.setResult(ResultEnum.FAILED);
        testResultRequest.setError(testExecutionResult.getThrowable().map(x -> x.toString()).orElse(null));
        break;
      case ABORTED:
        testResultRequest.setResult(ResultEnum.FAILED);
        testResultRequest.setError(testExecutionResult.getThrowable().map(x -> x.toString()).orElse(null));

        break;
      case SUCCESSFUL:
        testResultRequest.setResult(ResultEnum.PASSED);
        break;
      default:
        testResultRequest.setResult(ResultEnum.FAILED);
        break;
    }
    if(commonData.isDisabled)
      return;
    testResultRequest.setStartTime(commonData.startTime);
    testResultRequest.setEndTime(commonData.endTime);
    testResultRequest.setGroupId("");
    testResultRequest.setDurationInMs(commonData.endTime-commonData.startTime);
    testResultRequest.setResultType(ResultTypeEnum.TEST_CASE);
    testResultRequest.setTargetId(commonData.currentTestCaseID);
    testResultRequest.setRetryCount(0);
    TestraRestClient.createResult(testResultRequest);
  }

  @Override
  public void reportingEntryPublished(TestIdentifier testIdentifier, ReportEntry entry) {
  }

}
