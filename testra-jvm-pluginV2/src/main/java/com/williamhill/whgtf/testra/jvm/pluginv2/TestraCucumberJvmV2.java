package com.williamhill.whgtf.testra.jvm.pluginv2;

import static com.williamhill.whgtf.testra.jvm.util.PropertyHelper.getEnv;
import static com.williamhill.whgtf.testra.jvm.util.PropertyHelper.prop;

import com.williamhill.whgtf.test.bnw.pojo.scenarios.BackgroundStep;
import com.williamhill.whgtf.test.bnw.pojo.scenarios.ScenarioRequest;
import com.williamhill.whgtf.test.bnw.pojo.scenarios.ScenarioResponse;
import com.williamhill.whgtf.testra.jvm.client.api.TestraRestClient;
import com.williamhill.whgtf.testra.jvm.util.JsonObject;
import com.williamhill.whgtf.testra.jvm.util.PropertyHelper;
import cucumber.api.HookType;
import cucumber.api.Result;
import cucumber.api.TestCase;
import cucumber.api.TestStep;
import cucumber.api.event.EventHandler;
import cucumber.api.event.EventPublisher;
import cucumber.api.event.TestCaseFinished;
import cucumber.api.event.TestCaseStarted;
import cucumber.api.event.TestSourceRead;
import cucumber.api.event.TestStepFinished;
import cucumber.api.event.TestStepStarted;
import cucumber.api.formatter.Formatter;
import cucumber.runner.UnskipableStep;
import gherkin.ast.Examples;
import gherkin.ast.Feature;
import gherkin.ast.ScenarioDefinition;
import gherkin.ast.ScenarioOutline;
import gherkin.ast.Step;
import gherkin.ast.TableRow;
import gherkin.pickles.PickleTag;
import io.qameta.allure.model.Parameter;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.util.ResultsUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestraCucumberJvmV2 implements Formatter{

  static {
    ClassLoader classLoader = TestraCucumberJvmV2.class.getClassLoader();
    PropertyHelper.loadProperties(getEnv() + ".environment.properties", classLoader);
    PropertyHelper.loadProperties("endpoints.properties", classLoader);
  }
  private final CucumberSourceUtils cucumberSourceUtils = new CucumberSourceUtils();
  private Feature currentFeature;
  private String currentFeatureFile;
  private TestCase currentTestCase;
  private final Map<String, String> scenarioUuids = new HashMap<>();
  private static final Logger LOGGER = LoggerFactory.getLogger(TestraCucumberJvmV2.class);
  private final EventHandler<TestSourceRead> featureStartedHandler = this::handleFeatureStartedHandler;
  private final EventHandler<TestCaseStarted> caseStartedHandler = this::handleTestCaseStarted;
  private final EventHandler<TestCaseFinished> caseFinishedHandler = this::handleTestCaseFinished;
  private final EventHandler<TestStepStarted> stepStartedHandler = this::handleTestStepStarted;
  private final EventHandler<TestStepFinished> stepFinishedHandler = this::handleTestStepFinished;
  private static TestraRestClient testraRestClient;
  private static String projectID;
  private List<StepTemplate> backgroundSteps = new ArrayList<>();
  private Map<String, String> uuIDToID = new HashMap<>();

  public TestraCucumberJvmV2(){
    projectID = getTestraRestClient().getProjectID(prop("project"));
    LOGGER.info("Project ID is " + projectID);
  }

  public TestraRestClient getTestraRestClient() {
    if (testraRestClient == null) {
      testraRestClient = new TestraRestClient();
    }
    return testraRestClient;
  }


  @Override
  public void setEventPublisher(final EventPublisher publisher) {
    publisher.registerHandlerFor(TestSourceRead.class, featureStartedHandler);

    publisher.registerHandlerFor(TestCaseStarted.class, caseStartedHandler);
    publisher.registerHandlerFor(TestCaseFinished.class, caseFinishedHandler);

    publisher.registerHandlerFor(TestStepStarted.class, stepStartedHandler);
    publisher.registerHandlerFor(TestStepFinished.class, stepFinishedHandler);
  }


  private void handleFeatureStartedHandler(final TestSourceRead event) {
    cucumberSourceUtils.addTestSourceReadEvent(event.uri, event);
    createScenarios(cucumberSourceUtils.getFeature(event.uri));
  }


  private void createScenarios(Feature feature)
  {
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
    for(ScenarioDefinition scenario : feature.getChildren()) {
      if(!scenario.getClass().getSimpleName().equals("Background")){
        if(scenario.getClass().getSimpleName().equals("Scenario")) {
          ScenarioRequest scenarioRequest = new ScenarioRequest();
          scenarioRequest.setName(scenario.getName());
          scenarioRequest.setBackgroundSteps(backgroundSteps.stream()
              .map(s -> new BackgroundStep().withIndex(s.getIndex())
                  .withText(s.getGherkinStep())).collect(Collectors.toList()));
          scenarioRequest.setSteps(scenario.getSteps().stream()
              .map(s -> new com.williamhill.whgtf.test.bnw.pojo.scenarios.Step()
                  .withIndex(s.getLocation().getLine() - 1 - scenario.getLocation().getLine())
                  .withText(s.getText()))
              .collect(Collectors.toList()));
          String scenarioID = JsonObject
              .jsonToObject(testraRestClient.addScenario(scenarioRequest).getPayload(),
                  ScenarioResponse.class).getId();
          final String[] completeGherkin = {""};
          scenario.getSteps().forEach(x -> {
            completeGherkin[0] += x.getKeyword() + x.getText();
          });
          uuIDToID.put(Utils.md5(scenario.getName() + completeGherkin[0]), scenarioID);
        }
        else{
          List<Examples> examples = ((ScenarioOutline) scenario).getExamples();
          for(int y = 0; y< ((ScenarioOutline) scenario).getExamples().get(0).getTableBody().size(); y++) {
            List<com.williamhill.whgtf.test.bnw.pojo.scenarios.Step> steps = scenario.getSteps().stream()
                .map(x -> new com.williamhill.whgtf.test.bnw.pojo.scenarios.Step()
                    .withIndex(x.getLocation().getLine() - 1 - scenario.getLocation().getLine())
                    .withText(x.getKeyword()+x.getText()))
                .collect(Collectors.toList());
            List<com.williamhill.whgtf.test.bnw.pojo.scenarios.Step> processedSteps = new ArrayList<>();
            for (int i = 0;i < ((ScenarioOutline) scenario).getExamples().get(0).getTableHeader().getCells()
                    .size(); i++) {
              final int ii = i;
              final int yy = y;
              processedSteps = steps.stream()
                  .map(x -> x.withText(x.getText()
                      .replace(
                          "<" + ((ScenarioOutline) scenario).getExamples().get(0).getTableHeader()
                              .getCells().get(ii).getValue() + ">",
                          ((ScenarioOutline) scenario).getExamples().get(0).getTableBody().get(yy)
                              .getCells().get(ii).getValue())))
              .collect(Collectors.toList());
            }
            ScenarioRequest scenarioRequest = new ScenarioRequest();
            String scenarioName = scenario.getName();
            for(int z = 0; z< ((ScenarioOutline)scenario).getExamples().get(0).getTableHeader().getCells().size(); z++){
              scenarioName = scenarioName.replace("<" + ((ScenarioOutline)scenario).getExamples().get(0).getTableHeader().getCells().get(z).getValue() + ">",
                  ((ScenarioOutline)scenario).getExamples().get(0).getTableBody().get(y).getCells().get(z).getValue());
            }
            scenarioRequest.setName(scenarioName);
            scenarioRequest.setFeatureName(feature.getName());
            scenarioRequest.setBackgroundSteps(backgroundSteps.stream()
                .map(s -> new BackgroundStep().withIndex(s.getIndex())
                    .withText(s.getGherkinStep())).collect(Collectors.toList()));
            scenarioRequest.setSteps(processedSteps);
            String scenarioID = JsonObject
                .jsonToObject(testraRestClient.addScenario(scenarioRequest).getPayload(),
                    ScenarioResponse.class).getId();
            final String[] completeGherkin = {""};
            scenario.getSteps().forEach(x -> {
              completeGherkin[0] += x.getKeyword() + x.getText();
            });
            uuIDToID.put(Utils.md5(scenario.getName() + completeGherkin[0]), scenarioID);
          }
        }
      }
    }
  }



  private void handleTestCaseStarted(final TestCaseStarted event) {
    currentFeatureFile = event.testCase.getUri();
    currentFeature = cucumberSourceUtils.getFeature(currentFeatureFile);

    currentTestCase = event.testCase;

    final Deque<PickleTag> tags = new LinkedList<>();
    tags.addAll(event.testCase.getTags());

    final LabelBuilder labelBuilder = new LabelBuilder(currentFeature, event.testCase, tags);

    final TestResult result = new TestResult()
        .withUuid(getTestCaseUuid(event.testCase))
        .withHistoryId(getHistoryId(event.testCase))
        .withName(event.testCase.getName())
        .withLabels(labelBuilder.getScenarioLabels())
        .withLinks(labelBuilder.getScenarioLinks());

    final ScenarioDefinition scenarioDefinition =
        cucumberSourceUtils.getScenarioDefinition(currentFeatureFile, currentTestCase.getLine());
    if (scenarioDefinition instanceof ScenarioOutline) {
      result.withParameters(
          getExamplesAsParameters((ScenarioOutline) scenarioDefinition)
      );
    }

    if (currentFeature.getDescription() != null && !currentFeature.getDescription().isEmpty()) {
      result.withDescription(currentFeature.getDescription());
    }

  }

  private String getTestCaseUuid(final TestCase testCase) {
    return scenarioUuids.computeIfAbsent(getHistoryId(testCase), it -> UUID.randomUUID().toString());
  }

  private String getStepUuid(final TestStep step) {
    return currentFeature.getName() + getTestCaseUuid(currentTestCase)
        + step.getPickleStep().getText() + step.getStepLine();
  }

  private String getHookStepUuid(final TestStep step) {
    return currentFeature.getName() + getTestCaseUuid(currentTestCase)
        + step.getHookType().toString() + step.getCodeLocation();
  }

  private String getHistoryId(final TestCase testCase) {
    final String testCaseLocation = testCase.getUri() + ":" + testCase.getLine();
    return Utils.md5(testCaseLocation);
  }
  private List<Parameter> getExamplesAsParameters(final ScenarioOutline scenarioOutline) {
    final Examples examples = scenarioOutline.getExamples().get(0);
    final TableRow row = examples.getTableBody().stream()
        .filter(example -> example.getLocation().getLine() == currentTestCase.getLine())
        .findFirst().get();
    return IntStream.range(0, examples.getTableHeader().getCells().size()).mapToObj(index -> {
      final String name = examples.getTableHeader().getCells().get(index).getValue();
      final String value = row.getCells().get(index).getValue();
      return new Parameter().withName(name).withValue(value);
    }).collect(Collectors.toList());
  }

  private void handleTestStepStarted(final TestStepStarted event) {
    if (!event.testStep.isHook()) {
      final String stepKeyword = Optional.ofNullable(
          cucumberSourceUtils.getKeywordFromSource(currentFeatureFile, event.testStep.getStepLine())
      ).orElse("UNDEFINED");

      final StepResult stepResult = new StepResult()
          .withName(String.format("%s %s", stepKeyword, event.testStep.getPickleStep().getText()))
          .withStart(System.currentTimeMillis());


//      event.testStep.getStepArgument().stream()
//          .filter(argument -> argument instanceof PickleTable)
//          .findFirst()
//          .ifPresent(table -> createDataTableAttachment((PickleTable) table));
    } else if (event.testStep.isHook() && event.testStep instanceof UnskipableStep) {
      final StepResult stepResult = new StepResult()
          .withName(event.testStep.getHookType().toString())
          .withStart(System.currentTimeMillis());

//      lifecycle.startStep(getTestCaseUuid(currentTestCase), getHookStepUuid(event.testStep), stepResult);
    }
  }

  private void handleTestStepFinished(final TestStepFinished event) {
    if (event.testStep.isHook() && event.testStep instanceof UnskipableStep) {
      handleHookStep(event);
    } else {
      handlePickleStep(event);
    }
  }

  private void handleHookStep(final TestStepFinished event) {
    final String uuid = getHookStepUuid(event.testStep);
    Consumer<StepResult> stepResult = result -> result.withStatus(translateTestCaseStatus(event.result));

    if (!Status.PASSED.equals(translateTestCaseStatus(event.result))) {
      final StatusDetails statusDetails = ResultsUtils.getStatusDetails(event.result.getError()).get();
      if (event.testStep.getHookType() == HookType.Before) {
        final TagParser tagParser = new TagParser(currentFeature, currentTestCase);
        statusDetails
            .withMessage("Before is failed: " + event.result.getError().getLocalizedMessage())
            .withFlaky(tagParser.isFlaky())
            .withMuted(tagParser.isMuted())
            .withKnown(tagParser.isKnown());
      }
      stepResult = result -> result
          .withStatus(translateTestCaseStatus(event.result))
          .withStatusDetails(statusDetails);
    }
  }

  private Status translateTestCaseStatus(final Result testCaseResult) {
    Status allureStatus;
    if (testCaseResult.getStatus() == Result.Type.UNDEFINED || testCaseResult.getStatus() == Result.Type.PENDING) {
      allureStatus = Status.SKIPPED;
    } else {
      try {
        allureStatus = Status.fromValue(testCaseResult.getStatus().lowerCaseName());
      } catch (IllegalArgumentException e) {
        allureStatus = Status.BROKEN;
      }
    }
    return allureStatus;
  }

  private void handleTestCaseFinished(final TestCaseFinished event) {
    final StatusDetails statusDetails =
        ResultsUtils.getStatusDetails(event.result.getError()).orElse(new StatusDetails());

//    if (statusDetails.getMessage() != null && statusDetails.getTrace() != null) {
//      lifecycle.updateTestCase(getTestCaseUuid(event.testCase), scenarioResult ->
//          scenarioResult
//              .withStatus(translateTestCaseStatus(event.result))
//              .withStatusDetails(statusDetails));
//    } else {
//      lifecycle.updateTestCase(getTestCaseUuid(event.testCase), scenarioResult ->
//          scenarioResult
//              .withStatus(translateTestCaseStatus(event.result)));
//    }
//
//    lifecycle.stopTestCase(getTestCaseUuid(event.testCase));
//    lifecycle.writeTestCase(getTestCaseUuid(event.testCase));
  }

  private void handlePickleStep(final TestStepFinished event) {
    final StatusDetails statusDetails;
//    if (event.result.getStatus() == Result.Type.UNDEFINED) {
//      statusDetails =
//          ResultsUtils.getStatusDetails(new PendingException("TODO: implement me"))
//              .orElse(new StatusDetails());
//      lifecycle.updateTestCase(getTestCaseUuid(currentTestCase), scenarioResult ->
//          scenarioResult
//              .withStatus(translateTestCaseStatus(event.result))
//              .withStatusDetails(statusDetails));
//    } else {
//      statusDetails =
//          ResultsUtils.getStatusDetails(event.result.getError())
//              .orElse(new StatusDetails());
//    }
//
//    final TagParser tagParser = new TagParser(currentFeature, currentTestCase);
//    statusDetails
//        .withFlaky(tagParser.isFlaky())
//        .withMuted(tagParser.isMuted())
//        .withKnown(tagParser.isKnown());
//
//    lifecycle.updateStep(getStepUuid(event.testStep), stepResult ->
//        stepResult.withStatus(translateTestCaseStatus(event.result)));
//    lifecycle.stopStep(getStepUuid(event.testStep));
  }

}
