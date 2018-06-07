package com.williamhill.whgtf.testra.jvm.plugin;

import static com.williamhill.whgtf.testra.jvm.util.PropertyHelper.getEnv;
import static com.williamhill.whgtf.testra.jvm.util.PropertyHelper.prop;

import com.williamhill.whgtf.test.bnw.pojo.executions.ExecutionResponse;
import com.williamhill.whgtf.test.bnw.pojo.scenarios.BackgroundStep;
import com.williamhill.whgtf.test.bnw.pojo.scenarios.ScenarioRequest;
import com.williamhill.whgtf.test.bnw.pojo.scenarios.ScenarioResponse;
import com.williamhill.whgtf.test.bnw.pojo.testresult.TestResultRequest;
import com.williamhill.whgtf.testra.jvm.client.api.TestraRestClient;
import com.williamhill.whgtf.testra.jvm.message.http.HttpResponseMessage;
import com.williamhill.whgtf.testra.jvm.util.JsonObject;
import com.williamhill.whgtf.testra.jvm.util.PropertyHelper;
import cucumber.runtime.StepDefinitionMatch;
import gherkin.formatter.Formatter;
import gherkin.formatter.Reporter;
import gherkin.formatter.model.Background;
import gherkin.formatter.model.Examples;
import gherkin.formatter.model.Feature;
import gherkin.formatter.model.Match;
import gherkin.formatter.model.Result;
import gherkin.formatter.model.Scenario;
import gherkin.formatter.model.ScenarioOutline;
import gherkin.formatter.model.Step;
import gherkin.formatter.model.Tag;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestraCucumberJvm implements Reporter, Formatter {

  static {
    ClassLoader classLoader = TestraCucumberJvm.class.getClassLoader();
    PropertyHelper.loadProperties(getEnv() + ".environment.properties", classLoader);
    PropertyHelper.loadProperties("endpoints.properties", classLoader);
  }
  private static final Logger LOGGER = LoggerFactory.getLogger(TestraCucumberJvm.class);
  private TestraRestClient testraRestClient;
  private static final String FAILED = "failed";
  private static final String PASSED = "passed";
  private static final String SKIPPED = "skipped";
  private static final String PENDING = "pending";
  private static String projectID;
  private Feature currentFeature;
  private Scenario currentScenario;
  private long startTime;
  private boolean isScenarioOutline = false;
  private int scenarioOutlineIndex;
  private String ScenarioID;
  private String ExecutionID;
  private Step currentStep;
  private ScenarioTemplate currentScenarioTemplate;
  private int stepIndex = 0;

  public TestraCucumberJvm(){

    projectID = getTestraRestClient().getProjectID(prop("project"));
    LOGGER.info("Project ID is " + projectID);
  }

  public TestraRestClient getTestraRestClient(){
    if(testraRestClient == null){
      testraRestClient = new TestraRestClient();
    }
    return testraRestClient;
  }

  @Override
  public void feature(final Feature feature) {
    this.currentFeature = feature;
  }

  @Override
  public void scenario(final Scenario scnr) {
  }

  @Override
  public void startOfScenarioLifeCycle(final Scenario scenario) {
    this.currentScenario = scenario;
    stepIndex = 0;
    currentScenarioTemplate = new ScenarioTemplate();
    currentScenarioTemplate.setName(scenario.getName());
    currentScenarioTemplate.setFeatureName(currentFeature.getName());

    final Deque<Tag> tags = new LinkedList<>();
    tags.addAll(scenario.getTags());
    tags.addAll(currentFeature.getTags());
    currentScenarioTemplate.setTags(tags);

    startTime = System.currentTimeMillis();
  }

    @Override
    public void endOfScenarioLifeCycle(final Scenario scenario) {

      long timeAtEnd = System.currentTimeMillis();

      List<BackgroundStep> backgroundSteps = currentScenarioTemplate.getBackgroundSteps()
          .stream()
          .map(s -> new BackgroundStep().withIndex(s.getIndex()).withText(s.getGherkinStep()))
          .collect(Collectors.toList());

      List<com.williamhill.whgtf.test.bnw.pojo.scenarios.Step> steps = currentScenarioTemplate.getBackgroundSteps()
          .stream()
          .map(s -> new com.williamhill.whgtf.test.bnw.pojo.scenarios.Step().withIndex(s.getIndex()).withText(s.getGherkinStep()))
          .collect(Collectors.toList());

      ScenarioRequest scenarioRequest = new ScenarioRequest()
          .withName(currentScenarioTemplate.getName())
          .withFeatureName(currentScenarioTemplate.getFeatureName())
          .withBackgroundSteps(backgroundSteps)
          .withSteps(steps);

      HttpResponseMessage httpResponseMessage = testraRestClient.addScenario(scenarioRequest);
      ScenarioResponse scenarioResponse = JsonObject
          .jsonToObject(httpResponseMessage.getPayload(), ScenarioResponse.class);
      ScenarioID = scenarioResponse.getId();
      createExecution();

      String status;
      if(currentScenarioTemplate.getIsFailed()){
        status = StatusEnum.FAILED.getValue();
      }
      else if(currentScenarioTemplate.getIsSkipped()){
        status = StatusEnum.SKIPPED.getValue();
      }
      else{
        status = StatusEnum.PASSED.getValue();
      }

      TestResultRequest testResultRequest = new TestResultRequest()
          .withResult(status)
          .withResultType("SCENARIO")
          .withDurationInMs((int)(timeAtEnd-startTime))
          .withEndTime(timeAtEnd)
          .withStartTime(startTime)
          .withTargetId(ScenarioID);
      if(currentScenarioTemplate.getIsFailed()){
        testResultRequest.setError(currentScenarioTemplate.getError().getErrorMessage());
      }
      testraRestClient.addTestResult(testResultRequest,ExecutionID);


    }

  private void createExecution(){
    if(ExecutionID == null) {
      HttpResponseMessage httpResponseMessage = testraRestClient.createExecution();
      ExecutionID = JsonObject
          .jsonToObject(httpResponseMessage.getPayload(), ExecutionResponse.class)
          .getId();
    }
  }


    @Override
  public void result(final Result result) {
    int scenarioLine;
    if(isScenarioOutline){
      scenarioLine = scenarioOutlineIndex;
    }
    else {
      scenarioLine = currentScenario.getLine();
    }
    StepTemplate stepTemplate = new StepTemplate();
    stepTemplate.setGherkinStep(currentStep.getKeyword()+currentStep.getName());
    stepTemplate.setIndex(stepIndex);
    if(!result.getStatus().equals("skipped")) {
      stepTemplate.setStepDuration(result.getDuration());
    }
    stepIndex++;
    stepTemplate.setLine(currentStep.getLine());
      switch (result.getStatus()) {
        case FAILED:
          stepTemplate.setStatus(StatusEnum.FAILED);
          currentScenarioTemplate.setError(setErrors(stepTemplate,result));
          currentScenarioTemplate.setIsFailed(true);
          break;
        case PENDING:
          stepTemplate.setStatus(StatusEnum.PENDING);
          break;
        case SKIPPED:
          stepTemplate.setStatus(StatusEnum.SKIPPED);
          currentScenarioTemplate.setIsSkipped(true);
          break;
        case PASSED:
          stepTemplate.setStatus(StatusEnum.PASSED);
          break;
        default:
          break;
      }
      if(scenarioLine>stepTemplate.getLine()) {
        currentScenarioTemplate.getBackgroundSteps().add(stepTemplate);
      }
      else{
        currentScenarioTemplate.getSteps().add(stepTemplate);
      }
  }

  private ErrorTemplate setErrors(StepTemplate stepTemplate,Result result)
  {
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
  }


    @Override
  public void match(final Match match) {
      if (match instanceof StepDefinitionMatch) {
        currentStep = Utils.extractStep((StepDefinitionMatch) match);
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
      scenarioOutlineIndex = so.getLine();
    }

    @Override
    public void examples(final Examples exmpls) {
      isScenarioOutline = true;
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
