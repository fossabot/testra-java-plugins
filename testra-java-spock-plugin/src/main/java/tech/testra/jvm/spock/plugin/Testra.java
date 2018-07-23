package tech.testra.jvm.spock.plugin;

import static tech.testra.jvm.commons.util.PropertyHelper.prop;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spockframework.runtime.AbstractRunListener;
import org.spockframework.runtime.extension.IGlobalExtension;
import org.spockframework.runtime.model.ErrorInfo;
import org.spockframework.runtime.model.IterationInfo;
import org.spockframework.runtime.model.SpecInfo;
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

public class Testra extends AbstractRunListener implements IGlobalExtension {

  private static final Logger LOGGER = LoggerFactory.getLogger(Testra.class);
  private final Long threadId = Thread.currentThread().getId();
  private CommonData commonData;
  private String projectID;
  private String executionID;

  public Testra(){
    PropertyHelper.loadPropertiesFromAbsolute(new File(".testra").getAbsolutePath());
    setup();
    if(Boolean.parseBoolean(prop("testra.disabled"))){
      commonData.isDisabled = true;
      return;
    }
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
  public void start() {

  }

  @Override
  public void visitSpec(SpecInfo specInfo) {
    specInfo.addListener(this);
  }

  @Override
  public void stop() {

  }

  @Override
  public void beforeIteration(final IterationInfo iteration) {
    commonData.startTime = System.currentTimeMillis();
    if(commonData.isDisabled)
      return;
    List<Label> labels = getLabels(iteration);
    TestcaseRequest testCaseRequest = new TestcaseRequest();
    testCaseRequest.setTags(labels.stream().map(Label::getValue).collect(Collectors.toList()));
    String dataValueHeader = "";
    for(Object value : iteration.getDataValues()){
      if(dataValueHeader.equals(""))
        dataValueHeader += " where " + ((String) value.toString());
      else
        dataValueHeader += "," + ((String) value.toString());
    }
    testCaseRequest.setName(iteration.getName() + dataValueHeader);
    testCaseRequest.setClassName(iteration.getDescription().getClassName().substring(iteration.getDescription().getClassName().lastIndexOf(".")+1));
    testCaseRequest.setNamespace(iteration.getFeature().getSpec().getPackage());
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
    if(commonData.isRetry &&!commonData.failedScenarioIDs.containsKey(commonData.currentTestCaseID)){
      LOGGER.info("Test has already passed in a previous test run");
      commonData.skip = true;
    }
    else{
      if(commonData.isRetry) {
        commonData.skip = false;
        commonData.resultCounter = commonData.failedRetryMap.get(commonData.currentTestCaseID);
      }
    }
  }

  @Override
  public void error(final ErrorInfo error){
    commonData.isFailure = true;
    commonData.failureMessage = error.getException().getMessage() + "\n" + getStackTraceAsString(error.getException());

  }

  @Override
  public void afterIteration(final IterationInfo iterationInfo){
    commonData.endTime = System.currentTimeMillis();
    if(commonData.isDisabled||commonData.skip)
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
    if(commonData.isRetry){
      testResultRequest.setRetryCount(commonData.resultCounter+1);
      commonData.resultCounter = 0;
      TestraRestClient.updateResult(commonData.failedScenarioIDs.get(commonData.currentTestCaseID),testResultRequest);
    }
    else
      TestraRestClient.createResult(testResultRequest);
  }


  private Label createLabel(final Tag tag) {
    return new Label().withName("tag").withValue(tag.value());
  }

  private List<Label> getLabels(final IterationInfo iterationInfo) {
    return Stream.of(
        getLabels(iterationInfo, Tag.class, this::createLabel)
    ).reduce(Stream::concat).orElseGet(Stream::empty).collect(Collectors.toList());
  }

  private <T extends Annotation> Stream<Label> getLabels(final IterationInfo iterationInfo, final Class<T> clazz,
      final Function<T, Label> extractor) {
    final List<Label> onFeature = getFeatureAnnotations(iterationInfo, clazz).stream()
        .map(extractor)
        .collect(Collectors.toList());
    if (!onFeature.isEmpty()) {
      return onFeature.stream();
    }
    return getSpecAnnotations(iterationInfo, clazz).stream()
        .map(extractor);
  }


  private <T extends Annotation> List<T> getAnnotationsOnMethod(final Description result, final Class<T> clazz) {
    final T annotation = result.getAnnotation(clazz);
    return Stream.concat(
        extractRepeatable(result, clazz).stream(),
        Objects.isNull(annotation) ? Stream.empty() : Stream.of(annotation)
    ).collect(Collectors.toList());
  }

  @SuppressWarnings("unchecked")
  private <T extends Annotation> List<T> extractRepeatable(final Description result, final Class<T> clazz) {
    if (clazz.isAnnotationPresent(Repeatable.class)) {
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

  private <T extends Annotation> List<T> getAnnotationsOnClass(final Description result, final Class<T> clazz) {
    return Stream.of(result)
        .map(Description::getTestClass)
        .map(testClass -> testClass.getAnnotationsByType(clazz))
        .flatMap(Stream::of)
        .collect(Collectors.toList());
  }

  private <T extends Annotation> List<T> getFeatureAnnotations(final IterationInfo iteration, final Class<T> clazz) {
    return getAnnotationsOnMethod(iteration.getFeature().getDescription(), clazz);
  }

  private <T extends Annotation> List<T> getSpecAnnotations(final IterationInfo iteration, final Class<T> clazz) {
    final SpecInfo spec = iteration.getFeature().getSpec();
    return getAnnotationsOnClass(spec.getDescription(), clazz);
  }

  private static String getStackTraceAsString(final Throwable throwable) {
    final StringWriter stringWriter = new StringWriter();
    throwable.printStackTrace(new PrintWriter(stringWriter));
    return stringWriter.toString();
  }


}
