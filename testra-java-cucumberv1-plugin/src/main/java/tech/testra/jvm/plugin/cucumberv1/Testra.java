package tech.testra.jvm.plugin.cucumberv1;

import static tech.testra.jvm.commons.util.PropertyHelper.prop;

import cucumber.runtime.StepDefinitionMatch;
import gherkin.formatter.Formatter;
import gherkin.formatter.Reporter;
import gherkin.formatter.model.*;
import gherkin.formatter.model.Result;
import gherkin.formatter.model.Scenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.testra.java.client.TestraRestClient;
import tech.testra.java.client.model.*;
import tech.testra.java.client.model.TestResultRequest.ResultTypeEnum;
import tech.testra.java.client.model.TestResultRequest.StatusEnum;
import tech.testra.jvm.commons.util.PropertyHelper;
import tech.testra.jvm.plugin.cucumberv1.templates.StepTemplate;
import tech.testra.jvm.plugin.cucumberv1.templates.ErrorTemplate;
import tech.testra.jvm.plugin.cucumberv1.templates.ScenarioTemplate;
import tech.testra.jvm.plugin.cucumberv1.utils.CommonData;
import tech.testra.jvm.plugin.cucumberv1.utils.CommonDataProvider;
import tech.testra.jvm.plugin.cucumberv1.utils.Utils;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;


public class Testra implements Reporter, Formatter {

  private static final Logger LOGGER = LoggerFactory.getLogger(Testra.class);

  private static final String FAILED = "failed";
  private static final String PASSED = "passed";
  private static final String SKIPPED = "skipped";
  private static final String PENDING = "pending";
  private static final String UNDEFINED = "undefined";

  private CommonData commonData;
  private static String projectID;
  private final Long threadId = Thread.currentThread().getId();

  static {
    File propfile = new File(".testra");
    if(propfile.exists()) {
      PropertyHelper.loadPropertiesFromAbsolute(new File(".testra").getAbsolutePath());
    }
    else{
      PropertyHelper.loadPropertiesFromAbsolute(new File("../.testra").getAbsolutePath());
    }
    TestraRestClient.setURLs(prop("host"));
    projectID = TestraRestClient.getProjectID(prop("project"));
    LOGGER.info("Project ID is " + projectID);
    createExecution();
  }

  public Testra() {
    setup();
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
    List<String> tags = commonData.currentFeature.getTags().stream().map(Tag::getName).collect(Collectors.toList());
    tags.addAll(scenario.getTags().stream().map(Tag::getName).collect(Collectors.toList()));
    scenarioRequest.setTags(tags);

    List<TestStep> backgroundSteps = commonData.currentScenarioTemplate.getBackgroundSteps()
        .stream()
        .map(s -> {TestStep testStep = new TestStep();
        testStep.setIndex(s.getIndex());
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

    tech.testra.java.client.model.Scenario scenario1 = TestraRestClient.createScenario(scenarioRequest);
    commonData.currentScenarioID = scenario1.getId();
    commonData.currentFeatureID = scenario1.getFeatureId();

    TestResultRequest testResultRequest = new TestResultRequest();
    testResultRequest.setStatus(resultToEnum(commonData.result));
    testResultRequest.setResultType(ResultTypeEnum.SCENARIO);
    testResultRequest.setDurationInMs(timeAtEnd - commonData.startTime);
    testResultRequest.setTargetId(commonData.currentScenarioID);
    testResultRequest.setGroupId(commonData.currentFeatureID);
    testResultRequest.setStartTime(commonData.startTime);
    testResultRequest.setEndTime(timeAtEnd);
    List<StepResult> results = commonData.currentScenarioTemplate.getBackgroundSteps().stream()
        .map(this::getStepResult).collect(Collectors.toList());

    results.addAll(commonData.currentScenarioTemplate.getSteps().stream()
        .map(this::getStepResult).collect(Collectors.toList()));
    testResultRequest.setStepResults(results);


    if (commonData.currentScenarioTemplate.getIsFailed()) {
      testResultRequest.setError(commonData.currentScenarioTemplate.getError().getErrorMessage());
      if(commonData.attachmentMimeType != null){
        Attachment attachment = new Attachment();
        attachment.setName(commonData.attachmentMimeType);
        attachment.setBase64EncodedByteArray(new String(Base64.getEncoder().encode(commonData.attachmentByteArray)));
        testResultRequest.setAttachments(Collections.singletonList(attachment));
      }
    }
    TestraRestClient.createResult(testResultRequest);
  }

  private StepResult getStepResult(StepTemplate x) {
    StepResult stepResult = new StepResult();
    stepResult.setIndex(x .getIndex());
    stepResult.setStatus(StepResult.StatusEnum.fromValue(x.getStatus().getStatus().toUpperCase()));
    if (x.getStatus().equals(StatusEnum.FAILED)) {
      stepResult.setError(commonData.currentScenarioTemplate.getError().getErrorMessage());
    }
    return stepResult;
  }

  private static void createExecution() {
    if (TestraRestClient.getExecutionid() == null) {
      TestraRestClient.setExecutionid(null);
    }
  }
  private StatusEnum resultToEnum(Result result){
    switch(result.getStatus()){
      case FAILED:
        return StatusEnum.FAILED;
      case PASSED:
        return StatusEnum.PASSED;
      case PENDING:
        return StatusEnum.PENDING;
      case SKIPPED:
        return StatusEnum.SKIPPED;
      case UNDEFINED:
        return StatusEnum.UNDEFINED;
      default:
        throw new IllegalArgumentException("Result type " + result.toString() + " not found");
    }

  }
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
    stepTemplate.setStatus(result);

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
    commonData.attachmentMimeType = string;
    commonData.attachmentByteArray = bytes;
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
  public void examples(final Examples examples) {
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
