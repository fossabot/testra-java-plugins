package com.williamhill.whgtf.testra.jvm.plugin;

import static com.williamhill.whgtf.testra.jvm.util.PropertyHelper.getEnv;
import static com.williamhill.whgtf.testra.jvm.util.PropertyHelper.prop;
import static java.util.stream.Collectors.toMap;

import com.williamhill.whgtf.test.bnw.pojo.executions.ExecutionResponse;
import com.williamhill.whgtf.test.bnw.pojo.scenarios.BackgroundStep;
import com.williamhill.whgtf.test.bnw.pojo.scenarios.ScenarioRequest;
import com.williamhill.whgtf.test.bnw.pojo.scenarios.ScenarioResponse;
import com.williamhill.whgtf.test.bnw.pojo.steps.StepRequest;
import com.williamhill.whgtf.test.bnw.pojo.testresult.TestResultRequest;
import com.williamhill.whgtf.testra.jvm.client.api.TestraRestClient;
import com.williamhill.whgtf.testra.jvm.message.http.HttpRequestMessage;
import com.williamhill.whgtf.testra.jvm.message.http.HttpResponseMessage;
import com.williamhill.whgtf.testra.jvm.util.JsonObject;
import com.williamhill.whgtf.testra.jvm.util.PropertyHelper;
import cucumber.runtime.StepDefinitionMatch;
import gherkin.formatter.Reporter;
import gherkin.formatter.Formatter;
import gherkin.formatter.model.Background;
import gherkin.formatter.model.Examples;
import gherkin.formatter.model.Feature;
import gherkin.formatter.model.Match;
import gherkin.formatter.model.Result;
import gherkin.formatter.model.Scenario;
import gherkin.formatter.model.ScenarioOutline;
import gherkin.formatter.model.Step;
import gherkin.formatter.model.Tag;
import io.qameta.allure.model.Status;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestraCucumberJvm implements Reporter, Formatter {

  enum StatusEnum{
    PASSED("PASSED"),FAILED("PASSED"),SKIPPED("PASSED"),PENDING("PENDING");

    private String value;

    StatusEnum(String value) {
      this.value = value;
    }


    public String getValue() {
      return value;
    }
  }

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
  private boolean isNullMatch = true;
  private Feature currentFeature;
  private Scenario currentScenario;
  private final Deque<Step> gherkinSteps = new LinkedList<>();
  private final Deque<Pair<Step,StatusEnum>> gherkinStepsNoExamples = new LinkedList<>();
  private long currentTime;
  private long startTime;
  private boolean isScenarioOutline = false;
  private int scenarioOutlineIndex;
  private String ScenarioID;
  private String ExecutionID;
  private Step currentStep;

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

    final Deque<Tag> tags = new LinkedList<>();
    tags.addAll(scenario.getTags());
    tags.addAll(currentFeature.getTags());

    currentTime = System.currentTimeMillis();
    startTime = System.currentTimeMillis();

  }

    @Override
    public void endOfScenarioLifeCycle(final Scenario scenario) {

      long timeAtEnd = System.currentTimeMillis();
      final StepUtils stepUtils = new StepUtils(currentFeature, currentScenario);

      int scenarioLine;
      if(isScenarioOutline){
        scenarioLine = scenarioOutlineIndex;
      }
      else {
        scenarioLine = scenario.getLine();
      }
      final List<String> backgroundSteps = gherkinStepsNoExamples.stream()
          .filter(s -> s.getKey().getLine() < scenarioLine)
          .map(s -> s.getKey().getKeyword() + s.getKey().getName())
          .collect(Collectors.toList());

      final List<String> steps = gherkinStepsNoExamples.stream()
          .filter(s -> s.getKey().getLine() > scenarioLine)
          .map(s -> s.getKey().getKeyword() + s.getKey().getName())
          .collect(Collectors.toList());

      List<StepRequest>backgroundStepRequests = new ArrayList<>();
      List<StepRequest>stepRequests = new ArrayList<>();

      Map<Integer, String> backgroundmap = IntStream.range(0, backgroundSteps.size())
          .boxed()
          .collect(toMap(Function.identity(),backgroundSteps::get));
      Iterator it = backgroundmap.entrySet().iterator();
      while (it.hasNext()) {
        Map.Entry pair = (Map.Entry)it.next();
        backgroundStepRequests.add(new StepRequest().withIndex((Integer)pair.getKey()).withText((String)pair.getValue()));
        it.remove();
      }

      Map<Integer, String> map = IntStream.range(0, steps.size())
          .boxed()
          .collect(toMap(Function.identity(),steps::get));
      it = map.entrySet().iterator();
      while (it.hasNext()) {
        Map.Entry pair = (Map.Entry)it.next();
        stepRequests.add(new StepRequest().withIndex((Integer)pair.getKey()).withText((String)pair.getValue()));
        it.remove();
      }

      ScenarioTemplate scenarioTemplate = new ScenarioTemplate();
      scenarioTemplate.setBackgroundSteps(backgroundStepRequests);
      scenarioTemplate.setSteps(stepRequests);
      scenarioTemplate.setFeatureName(currentFeature.getName());
      scenarioTemplate.setName(currentScenario.getName());

      List<BackgroundStep> backgroundSteps1 = new ArrayList<>();
      List<com.williamhill.whgtf.test.bnw.pojo.scenarios.Step> steps1 = new ArrayList<>();

      scenarioTemplate.getBackgroundSteps().stream()
          .forEach(s -> {
            backgroundSteps1.add(new BackgroundStep().withIndex(s.getIndex()).withText(s.getText()));
          });

      scenarioTemplate.getSteps().stream()
          .forEach(s -> {
            steps1.add(new com.williamhill.whgtf.test.bnw.pojo.scenarios.Step().withIndex(s.getIndex()).withText(s.getText()));
          });

      ScenarioRequest scenarioRequest = new ScenarioRequest()
          .withName(scenarioTemplate.getName())
          .withFeatureName(scenarioTemplate.getFeatureName())
          .withBackgroundSteps(backgroundSteps1)
          .withSteps(steps1);

      HttpResponseMessage httpResponseMessage = testraRestClient.getScenarios();
      List<ScenarioResponse> scenarioList = JsonObject.jsonToList(httpResponseMessage.getPayload(), ScenarioResponse.class);
      List<ScenarioResponse> filteredScenarioList = scenarioList.stream()
          .filter(s -> s.getName().equals(scenarioRequest.getName()))
          .collect(Collectors.toList());
      StringBuilder stepsum = new StringBuilder();
      for (String s : scenarioRequest.getSteps().stream().map(x -> x.getText()).collect(Collectors.toList()))
      {
        stepsum.append(s);
        stepsum.append("\t");
      }

      String targetsum = filteredScenarioList.stream()
          .filter(
              x -> {
                StringBuilder sum = new StringBuilder();
                for (String s : x.getSteps().stream().map(y -> y.getText()).collect(Collectors.toList()))
                {
                  sum.append(s);
                  sum.append("\t");
                }

                if(sum.toString().equals(stepsum.toString())){
                  return true;
                }
                return false;
              }
          )
          .map(ScenarioResponse::getId)
          .findFirst()
          .orElse(null);

      if(targetsum==null) {
        httpResponseMessage = testraRestClient.addScenario(scenarioRequest);
        ScenarioResponse scenarioResponse = JsonObject
            .jsonToObject(httpResponseMessage.getPayload(), ScenarioResponse.class);
        ScenarioID = scenarioResponse.getId();
      }
      else{
        ScenarioID = targetsum;
      }
      createExecution();

      TestResultRequest testResultRequest = new TestResultRequest()
          .withResult(gherkinStepsNoExamples.getLast().getValue().getValue())
          .withResultType("SCENARIO")
          .withDurationInMs((int)(timeAtEnd-startTime))
          .withEndTime(timeAtEnd)
          .withStartTime(startTime)
          .withTargetId(ScenarioID)
          .withError("NOT ADDED YET");
      gherkinStepsNoExamples.clear();
      httpResponseMessage = testraRestClient.addTestResult(testResultRequest,ExecutionID);


      synchronized (gherkinSteps) {
        while (gherkinSteps.peek() != null) {
          stepUtils.fireCanceledStep(gherkinSteps.remove());
        }
      }
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
      switch (result.getStatus()) {
        case FAILED:
          gherkinStepsNoExamples.add(Pair.of(currentStep,StatusEnum.FAILED));
          break;
        case PENDING:
          gherkinStepsNoExamples.add(Pair.of(currentStep,StatusEnum.PENDING));
          break;
        case SKIPPED:
          gherkinStepsNoExamples.add(Pair.of(currentStep,StatusEnum.SKIPPED));
          break;
        case PASSED:
          gherkinStepsNoExamples.add(Pair.of(currentStep,StatusEnum.PASSED));
          break;
        default:
          break;
      }
  }

  @Override
  public void after(final Match match, final Result result) {
    LOGGER.info("here");
  }

  @Override
  public void step(final Step step) {
    long time = System.currentTimeMillis();
    LOGGER.info("DURATION: " + (time - currentTime));
    currentTime = time;
    synchronized (gherkinSteps) {
      gherkinSteps.add(step);
    }
  }


    @Override
  public void match(final Match match) {
    final StepUtils stepUtils = new StepUtils(currentFeature, currentScenario);
      if (match instanceof StepDefinitionMatch) {
        isNullMatch = false;
        final Step step = stepUtils.extractStep((StepDefinitionMatch) match);
        currentStep = step;
        synchronized (gherkinSteps) {
          while (gherkinSteps.peek() != null && !stepUtils.isEqualSteps(step, gherkinSteps.peek())) {
            stepUtils.fireCanceledStep(gherkinSteps.remove());
          }
          if (stepUtils.isEqualSteps(step, gherkinSteps.peek())) {
            gherkinSteps.remove();
          }
        }

      }
  }

  @Override
  public void before(final Match match, final Result result) {
  }

    @Override
    public void embedding(final String string, final byte[] bytes) {
      //Nothing to do with Allure
    }

    @Override
    public void write(final String string) {
      //Nothing to do with Allure
    }

    @Override
    public void syntaxError(final String state, final String event,
        final List<String> legalEvents, final String uri, final Integer line) {
      //Nothing to do with Allure
    }

    @Override
    public void uri(final String uri) {
      //Nothing to do with Allure
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
      //Nothing to do with Allure
    }

    @Override
    public void done() {
      //Nothing to do with Allure
    }

    @Override
    public void close() {
      //Nothing to do with Allure
    }

    @Override
    public void eof() {
      //Nothing to do with Allure

    }






}
