package tech.testra.jvm.plugin.junit4;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.testra.java.client.TestraRestClient;
import tech.testra.java.client.model.EnrichedTestResult;
import tech.testra.java.client.model.TestResultRequest;
import tech.testra.java.client.model.TestResultRequest.ResultTypeEnum;
import tech.testra.java.client.model.TestResultRequest.StatusEnum;
import tech.testra.java.client.model.Testcase;
import tech.testra.java.client.model.TestcaseRequest;
import tech.testra.jvm.commons.Label;
import tech.testra.jvm.commons.Tag;
import tech.testra.jvm.commons.util.CommonData;
import tech.testra.jvm.commons.util.CommonDataProvider;
import tech.testra.jvm.commons.util.PropertyHelper;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static tech.testra.jvm.commons.util.PropertyHelper.prop;

public class Testra extends RunListener {

  private static final Logger LOGGER = LoggerFactory.getLogger(Testra.class);
  private final Long threadId = Thread.currentThread().getId();
  private CommonData commonData;
  private static String projectID;
  private static String executionID;
  private static boolean isDisabled;
  private static boolean isRetry;
  private static boolean setExecutionID;
  private static Map<String,String> failedScenarioIDs = new HashMap<>();
  private static Map<String,Integer> failedRetryMap = new HashMap<>();

  static {
    File propfile = new File(".testra");
    if(propfile.exists()) {
      PropertyHelper.loadPropertiesFromAbsolute(new File(".testra").getAbsolutePath());
    }
    else{
      PropertyHelper.loadPropertiesFromAbsolute(new File("../.testra").getAbsolutePath());
    }
    if(Boolean.parseBoolean(prop("testra.disabled"))){
      isDisabled = true;
    }
    else{
      TestraRestClient.setURLs(prop("host"));
      projectID = TestraRestClient.getProjectID(prop("project"));
      LOGGER.info("Project ID is " + projectID);
      isRetry = Boolean.parseBoolean(prop("isrerun"));
      setExecutionID=Boolean.parseBoolean(prop("setExecutionID"));
      if(prop("buildRef")!=null){
        TestraRestClient.buildRef = prop("buildRef");
      }
      if(prop("testra.execution.description")!=null){
        TestraRestClient.executionDescription = prop("testra.execution.description");
      }
      createExecution();
      if(isRetry) {
        List<EnrichedTestResult> failedTests = TestraRestClient.getFailedResults();
        failedTests.forEach(x -> {failedScenarioIDs.put(x.getTargetId(),x.getId());
          failedRetryMap.put(x.getTargetId(), x.getRetryCount());});
      }
    }

  }

  public Testra(){
    setup();
  }

  private static void createExecution() {
    if(TestraRestClient.getExecutionid() == null) {
      if(isRetry){
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
      else if(setExecutionID){
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
  public void testRunStarted(Description description) throws Exception {
    if(isDisabled)
      return;
  }

  @Override
  public void testRunFinished(Result result) throws Exception {
    if(isDisabled)
      return;
  }

  @Override
  public void testStarted(Description description) throws Exception {
    commonData.startTime = System.currentTimeMillis();
    if(isDisabled)
      return;
    List<Label> labels = getLabels(description);
    TestcaseRequest testCaseRequest = new TestcaseRequest();
    testCaseRequest.setTags(labels.stream().map(Label::getValue).collect(Collectors.toList()));
    testCaseRequest.setName(description.getMethodName());
    testCaseRequest.setClassName(description.getClassName().substring(description.getClassName().lastIndexOf(".")+1));
    testCaseRequest.setNamespace(description.getClassName().substring(0,description.getClassName().lastIndexOf(".")));
    if(labels.stream().map(Label::getValue).collect(Collectors.toList()).contains("Manual")){
      commonData.isManual = true;
    }
    else{
      commonData.isManual = false;
    }
    if(labels.stream().map(Label::getValue).collect(Collectors.toList()).contains("ExpectedFailure")){
      commonData.isExpectedFailure = true;
    }
    else{
      commonData.isExpectedFailure = false;
    }
    testCaseRequest.setManual(commonData.isManual);
    Testcase testcase = TestraRestClient.createTestcase(testCaseRequest);
    commonData.currentTestCaseID = testcase.getId();
    commonData.currentGroupID = testcase.getNamespaceId();
    if(isRetry &&!failedScenarioIDs.containsKey(commonData.currentTestCaseID)){
      LOGGER.info("Test has already passed in a previous test run");
      commonData.skip = true;
    }
    else{
      if(isRetry) {
        commonData.skip = false;
        commonData.resultCounter = failedRetryMap.get(commonData.currentTestCaseID);
      }
    }
  }

  @Override
  public void testFinished(Description description) throws Exception {
    commonData.endTime = System.currentTimeMillis();
    if(isDisabled||commonData.skip)
      return;
    TestResultRequest testResultRequest = new TestResultRequest();
    if(commonData.isManual){
      testResultRequest.setStartTime(0L);
      testResultRequest.setEndTime(0L);
      testResultRequest.setGroupId(commonData.currentGroupID);
      testResultRequest.setDurationInMs(0L);
      testResultRequest.setResultType(ResultTypeEnum.TEST_CASE);
      testResultRequest.setTargetId(commonData.currentTestCaseID);
      testResultRequest.setRetryCount(0);
      testResultRequest.setStatus(StatusEnum.SKIPPED);
      testResultRequest.setExpectedToFail(commonData.isExpectedFailure);
      TestraRestClient.createResult(testResultRequest);
      return;
    }
    if(commonData.isFailure){
      testResultRequest.setStatus(StatusEnum.FAILED);
      testResultRequest.setError(commonData.failureMessage);
      commonData.isFailure = false;
    }
    else if(commonData.isIgnored){
      testResultRequest.setStatus(StatusEnum.SKIPPED);
      commonData.isIgnored = false;
    }
    else{
      testResultRequest.setStatus(StatusEnum.PASSED);
    }
    testResultRequest.setStartTime(commonData.startTime);
    testResultRequest.setEndTime(commonData.endTime);
    testResultRequest.setGroupId(commonData.currentGroupID);
    testResultRequest.setDurationInMs(commonData.endTime-commonData.startTime);
    testResultRequest.setResultType(ResultTypeEnum.TEST_CASE);
    testResultRequest.setTargetId(commonData.currentTestCaseID);
    testResultRequest.setExpectedToFail(commonData.isExpectedFailure);
    testResultRequest.setRetryCount(0);
    if(isRetry){
      testResultRequest.setRetryCount(commonData.resultCounter+1);
      commonData.resultCounter = 0;
      TestraRestClient.updateResult(failedScenarioIDs.get(commonData.currentTestCaseID),testResultRequest);
    }
    else
      TestraRestClient.createResult(testResultRequest);
  }

  @Override
  public void testFailure(Failure failure) throws Exception {
    if(isDisabled)
      return;
    commonData.failureMessage = failure.getMessage() + failure.getTrace();
    commonData.isFailure = true;
  }

  @Override
  public void testAssumptionFailure(Failure failure) {
    if(isDisabled)
      return;
    commonData.failureMessage = failure.getMessage() + failure.getTrace();
    commonData.isFailure = true;
  }

  @Override
  public void testIgnored(Description description) throws Exception {
    if(isDisabled)
      return;
    commonData.isIgnored = true;
  }

  private List<Label> getLabels(final Description result) {
    return Stream.of(
        getLabels(result, Tag.class, this::createLabel)
    ).reduce(Stream::concat).orElseGet(Stream::empty).collect(Collectors.toList());
  }

  private Label createLabel(final Tag tag) {
    return new Label().withName("tag").withValue(tag.value());
  }

  private <T extends Annotation> Stream<Label> getLabels(final Description result, final Class<T> labelAnnotation,
      final Function<T, Label> extractor) {

    final List<Label> labels = getAnnotationsOnMethod(result, labelAnnotation).stream()
        .map(extractor)
        .collect(Collectors.toList());

    if (labelAnnotation.isAnnotationPresent(Repeatable.class) || labels.isEmpty()) {
      final Stream<Label> onClassLabels = getAnnotationsOnClass(result, labelAnnotation).stream()
          .map(extractor);
      labels.addAll(onClassLabels.collect(Collectors.toList()));
    }

    return labels.stream();
  }

  private <T extends Annotation> List<T> getAnnotationsOnClass(final Description result, final Class<T> clazz) {
    return Stream.of(result)
        .map(Description::getTestClass)
        .filter(Objects::nonNull)
        .map(testClass -> testClass.getAnnotationsByType(clazz))
        .flatMap(Stream::of)
        .collect(Collectors.toList());
  }

  private <T extends Annotation> List<T> getAnnotationsOnMethod(final Description result, final Class<T> clazz) {
    final T annotation = result.getAnnotation(clazz);
    return Stream.concat(
        extractRepeatable(result, clazz).stream(),
        Objects.isNull(annotation) ? Stream.empty() : Stream.of(annotation)
    ).collect(Collectors.toList());
  }

  private <T extends Annotation> List<T> extractRepeatable(final Description result, final Class<T> clazz) {
    if (clazz != null && clazz.isAnnotationPresent(Repeatable.class)) {
      final Repeatable repeatable = clazz.getAnnotation(Repeatable.class);
      final Class<? extends Annotation> wrapper = repeatable.value();
      final Annotation annotation = result.getAnnotation(wrapper);
      if (Objects.nonNull(annotation)) {
        try {
          final Method value = annotation.getClass().getMethod("value");
          final Object annotations = value.invoke(annotation);
          return Arrays.asList((T[]) annotations);
        } catch (Exception e) {
          throw new IllegalStateException(e);
        }
      }
    }
    return Collections.emptyList();
  }
}
