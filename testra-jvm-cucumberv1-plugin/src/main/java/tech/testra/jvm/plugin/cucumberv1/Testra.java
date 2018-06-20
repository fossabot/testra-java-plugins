package tech.testra.jvm.plugin.cucumberv1;

import cucumber.runtime.StepDefinitionMatch;
import gherkin.formatter.Formatter;
import gherkin.formatter.Reporter;
import gherkin.formatter.model.*;
import java.io.File;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.testra.jvm.apiv2.client.api.TestraRestClientV2;
import tech.testra.jvm.apiv2.util.PropertyHelper;
import tech.testra.jvm.client.model.ScenarioRequest;
import tech.testra.jvm.client.model.StepResult;
import tech.testra.jvm.client.model.TestResultRequest;
import tech.testra.jvm.client.model.TestResultRequest.ResultEnum;
import tech.testra.jvm.client.model.TestResultRequest.ResultTypeEnum;
import tech.testra.jvm.client.model.TestStep;
import tech.testra.jvm.plugin.cucumberv1.enums.StatusEnum;
import tech.testra.jvm.plugin.cucumberv1.templates.ErrorTemplate;
import tech.testra.jvm.plugin.cucumberv1.templates.ScenarioTemplate;
import tech.testra.jvm.plugin.cucumberv1.templates.StepTemplate;
import tech.testra.jvm.plugin.cucumberv1.utils.CommonData;
import tech.testra.jvm.plugin.cucumberv1.utils.CommonDataProvider;
import tech.testra.jvm.plugin.cucumberv1.utils.Utils;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static tech.testra.jvm.apiv2.util.PropertyHelper.getEnv;
import static tech.testra.jvm.apiv2.util.PropertyHelper.prop;

public class Testra implements Reporter, Formatter {

  private static final Logger LOGGER = LoggerFactory.getLogger(Testra.class);

  private static final String FAILED = "failed";
  private static final String PASSED = "passed";
  private static final String SKIPPED = "skipped";
  private static final String PENDING = "pending";
  private static final String UNDEFINED = "undefined";
  private String executionID;

  private CommonData commonData;
  private static String projectID;
  private final Long threadId = Thread.currentThread().getId();

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
    commonData.getTestraRestClientV2().setURLs(prop("host"));
    projectID = commonData.getTestraRestClientV2().getProjectID(prop("project"));
    LOGGER.info("Project ID is " + projectID);
    createExecution();
  }

  public Testra() {
    PropertyHelper.loadProperties(getEnv() + ".environment.properties", Testra.class.getClassLoader());
    setup();
    commonData.getTestraRestClientV2().setURLs(prop("host"));
    projectID = commonData.getTestraRestClientV2().getProjectID(prop("project"));
    LOGGER.info("Project ID is " + projectID);
    createExecution();
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


  @Override
  public void feature(final Feature feature) {
    commonData.currentFeature = feature;
  }

  @Override
  public void scenario(final Scenario scnr) {
  }

  @Override
  public void startOfScenarioLifeCycle(final Scenario scenario) {
    commonData.currentScenario = scenario;
    commonData.stepIndex = 0;
    commonData.currentScenarioTemplate = new ScenarioTemplate();
    commonData.currentScenarioTemplate.setName(scenario.getName());
    commonData.currentScenarioTemplate.setFeatureName(commonData.currentFeature.getName());

    final Deque<Tag> tags = new LinkedList<>();
    tags.addAll(scenario.getTags());
    tags.addAll(commonData.currentFeature.getTags());
    commonData.currentScenarioTemplate.setTags(tags);

    commonData.startTime = System.currentTimeMillis();
  }

  @Override
  public void endOfScenarioLifeCycle(final Scenario scenario) {
    long timeAtEnd = System.currentTimeMillis();
    ScenarioRequest scenarioRequest = new ScenarioRequest();
    scenarioRequest.setFeatureName(commonData.currentFeature.getName());
    scenarioRequest.setFeatureDescription(commonData.currentFeature.getDescription());
    scenarioRequest.setName(scenario.getName());
    List<String> tags = commonData.currentFeature.getTags().stream().map(x -> x.getName()).collect(Collectors.toList());
    tags.addAll(scenario.getTags().stream().map(x -> x.getName()).collect(Collectors.toList()));
    scenarioRequest.setTags(tags);

    List<TestStep> backgroundSteps = commonData.currentScenarioTemplate.getBackgroundSteps()
        .stream()
        .map(s -> {TestStep testStep = new TestStep(); testStep.setIndex(s.getIndex());
        testStep.setText(s.getGherkinStep());
        return testStep;})
        .collect(Collectors.toList());
    commonData.stepIndex = 0;

    List<TestStep> steps = commonData.currentScenarioTemplate.getSteps()
        .stream()
        .map(s -> {TestStep testStep = new TestStep(); testStep.setIndex(s.getIndex());
          testStep.setText(s.getGherkinStep());
          return testStep;})
        .collect(Collectors.toList());
    commonData.stepIndex = 0;

    scenarioRequest.setBackgroundSteps(backgroundSteps);
    scenarioRequest.setSteps(steps);

    String scenarioID = commonData.getTestraRestClientV2().createScenario(scenarioRequest);

    TestResultRequest testResultRequest = new TestResultRequest();
    testResultRequest.setResult(resultToEnum(commonData.result));
    testResultRequest.setResultType(ResultTypeEnum.SCENARIO);
    testResultRequest.setDurationInMs(timeAtEnd - commonData.startTime);
    testResultRequest.setTargetId(scenarioID);
    testResultRequest.setStartTime(commonData.startTime);
    testResultRequest.setEndTime(timeAtEnd);
    List<StepResult> results = commonData.currentScenarioTemplate.getBackgroundSteps().stream()
        .map(this::getStepResult).collect(Collectors.toList());

    results.addAll(commonData.currentScenarioTemplate.getSteps().stream()
        .map(this::getStepResult).collect(Collectors.toList()));
    testResultRequest.setStepResults(results);


    if (commonData.currentScenarioTemplate.getIsFailed()) {
      testResultRequest.setError(commonData.currentScenarioTemplate.getError().getErrorMessage());
    }
    commonData.getTestraRestClientV2().createResult(testResultRequest);
  }

  private StepResult getStepResult(StepTemplate x) {
    StepResult stepResult = new StepResult();
    stepResult.setIndex(x .getIndex());
    stepResult.setResult(resultToTestResult(x.getStatus()));
    if (x.getStatus().equals(ResultEnum.FAILED)) {
      stepResult.setError(commonData.currentScenarioTemplate.getError().getErrorMessage());
    }
    return stepResult;
  }

  private void createExecution() {
    if (executionID == null) {
      executionID = commonData.getTestraRestClientV2().createExecution();
    }
  }
  private ResultEnum resultToEnum(Result result){
    switch(result.getStatus()){
      case FAILED:
        return ResultEnum.FAILED;
      case PASSED:
        return ResultEnum.PASSED;
      case PENDING:
        return ResultEnum.PENDING;
      case SKIPPED:
        return ResultEnum.SKIPPED;
      case UNDEFINED:
        return ResultEnum.UNDEFINED;
      default:
        throw new IllegalArgumentException("Result type " + result.toString() + " not found");
    }

  }

  private StepResult.ResultEnum resultToTestResult(ResultEnum resultEnum){
    switch(resultEnum){
      case FAILED:
        return StepResult.ResultEnum.FAILED;
      case PASSED:
        return StepResult.ResultEnum.PASSED;
      case PENDING:
        return StepResult.ResultEnum.PENDING;
      case SKIPPED:
        return StepResult.ResultEnum.SKIPPED;
      case UNDEFINED:
        return StepResult.ResultEnum.UNDEFINED;
      default:
        throw new IllegalArgumentException("Result type " + resultEnum.toString() + " not found");
    }

  }


//  private StepResult.ResultEnum StepResultToEnum(Result result){
//    switch(result){
//      case FAILED:
//        return StepResult.ResultEnum.FAILED;
//      case PASSED:
//        return StepResult.ResultEnum.PASSED;
//      case PENDING:
//        return StepResult.ResultEnum.PENDING;
//      case SKIPPED:
//        return StepResult.ResultEnum.SKIPPED;
//      case AMBIGUOUS:
//        return StepResult.ResultEnum.AMBIGUOUS;
//      case UNDEFINED:
//        return StepResult.ResultEnum.UNDEFINED;
//      default:
//        throw new IllegalArgumentException("Result type " + result.toString() + " not found");
//    }
//  }

  @Override
  public void result(final Result result) {
    commonData.result = result;
    int scenarioLine;
    if (commonData.isScenarioOutline) {
      scenarioLine = commonData.scenarioOutlineIndex;
    } else {
      scenarioLine = commonData.currentScenario.getLine();
    }
    StepTemplate stepTemplate = new StepTemplate();
    stepTemplate.setGherkinStep(commonData.currentStep.getKeyword() + commonData.currentStep.getName());
    stepTemplate.setIndex(commonData.stepIndex);
    if (!result.getStatus().equals("skipped")&&!result.getStatus().equals("undefined")) {
      stepTemplate.setStepDuration(result.getDuration());
    }
    commonData.stepIndex++;
    stepTemplate.setLine(commonData.currentStep.getLine());
    if(result.getStatus().equals(FAILED)){
      commonData.currentScenarioTemplate.setError(setErrors(stepTemplate, result));
      commonData.currentScenarioTemplate.setIsFailed(true);
    }
    stepTemplate.setStatus(resultToEnum(result));

    if (scenarioLine > stepTemplate.getLine()) {
      commonData.currentScenarioTemplate.getBackgroundSteps().add(stepTemplate);
    } else {
      commonData.currentScenarioTemplate.getSteps().add(stepTemplate);
    }
  }

  private ErrorTemplate setErrors(StepTemplate stepTemplate, Result result) {
    ErrorTemplate errorTemplate = new ErrorTemplate();
    errorTemplate.setGherkinStep(stepTemplate.getGherkinStep());
    errorTemplate.setLineNumber(stepTemplate.getLine());
    errorTemplate.setErrorMessage(result.getErrorMessage());
    errorTemplate.setLocalizedErrorMessage(result.getError().getLocalizedMessage());
    errorTemplate.setStackTrace(result.getError().getStackTrace());
    return errorTemplate;
  }

  @Override
  public void after(final Match match, final Result result) {
  }

  @Override
  public void step(final Step step) {
    commonData.currentStep = step;
  }


  @Override
  public void match(final Match match) {
    if (match instanceof StepDefinitionMatch) {
      commonData.currentStep = Utils.extractStep((StepDefinitionMatch) match);
    }
  }

  @Override
  public void before(final Match match, final Result result) {
  }

  @Override
  public void embedding(final String string, final byte[] bytes) {
  }

  @Override
  public void write(final String string) {
  }

  @Override
  public void syntaxError(final String state, final String event,
      final List<String> legalEvents, final String uri, final Integer line) {
  }

  @Override
  public void uri(final String uri) {
  }

  @Override
  public void scenarioOutline(final ScenarioOutline so) {
    commonData.scenarioOutlineIndex = so.getLine();
  }

  @Override
  public void examples(final Examples exmpls) {
    commonData.isScenarioOutline = true;
  }


  @Override
  public void background(final Background b) {
  }

  @Override
  public void done() {
  }

  @Override
  public void close() {
  }

  @Override
  public void eof() {
  }

}
