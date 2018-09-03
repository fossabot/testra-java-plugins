package tech.testra.jvm.plugin.cucumberv3;

import cucumber.api.PickleStepTestStep;
import cucumber.api.Result.Type;
import cucumber.api.event.*;
import cucumber.api.formatter.Formatter;
import gherkin.ast.DataTable;
import gherkin.ast.Feature;
import gherkin.ast.Node;
import gherkin.ast.ScenarioDefinition;
import gherkin.ast.Step;
import gherkin.ast.TableCell;
import gherkin.ast.TableRow;
import gherkin.pickles.PickleCell;
import gherkin.pickles.PickleRow;
import gherkin.pickles.PickleTable;
import java.io.FileWriter;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.reporters.Files;
import tech.testra.java.client.TestraRestClient;
import tech.testra.java.client.model.*;
import tech.testra.java.client.model.TestResultRequest.ResultTypeEnum;
import tech.testra.java.client.model.TestResultRequest.StatusEnum;
import tech.testra.jvm.commons.util.MD5;
import tech.testra.jvm.commons.util.PropertyHelper;
import tech.testra.jvm.plugin.cucumberv3.utils.CommonData;
import tech.testra.jvm.plugin.cucumberv3.utils.CommonDataProvider;
import tech.testra.jvm.plugin.cucumberv3.utils.CucumberSourceUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import tech.testra.jvm.plugin.cucumberv3.utils.StepTemplate;

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
  private EventHandler<EmbedEvent> embedEventhandler = this::handleEmbed;
  private String projectID;
  private String executionID;

  public Testra() {
    PropertyHelper.loadPropertiesFromAbsolute(new File(".testra").getAbsolutePath());
    setup();
    if (Boolean.parseBoolean(prop("testra.disabled", "false"))) {
      featureStartedHandler = this::handleFeatureStartedHandlerDisabled;
      caseStartedHandler = this::handleTestCaseStartedDisabled;
      caseFinishedHandler = this::handleTestCaseFinishedDisabled;
      stepStartedHandler = this::handleTestStepStartedDisabled;
      stepFinishedHandler = this::handleTestStepFinishedDisabled;
      snippetsSuggestedEventEventHandler = this::snippetHandler;
    } else {
      featureStartedHandler = this::handleFeatureStartedHandler;
      caseStartedHandler = this::handleTestCaseStarted;
      caseFinishedHandler = this::handleTestCaseFinished;
      stepStartedHandler = this::handleTestStepStarted;
      stepFinishedHandler = this::handleTestStepFinished;
      snippetsSuggestedEventEventHandler = this::snippetHandler;
      setProperties();
    }

  }

  private void handleFeatureStartedHandlerDisabled(final TestSourceRead event) {

  }

  private void handleTestCaseStartedDisabled(TestCaseStarted event) {

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

  private void setup() {
    LOGGER.info("Scenario setup - Begin");
    CommonDataProvider.newCommonData(threadId);
    if ((commonData = CommonDataProvider.get(threadId)) == null) {
      throw new RuntimeException("Common data not found for id : " + threadId);
    } else {
      LOGGER.info("Initialised common data instance for scenario");
    }
    LOGGER.info("Scenario setup - End");
  }

  private void setProperties() {
    TestraRestClient.setURLs(prop("host"));
    projectID = TestraRestClient.getProjectID(prop("project"));
    LOGGER.info("Project ID is " + projectID);
    commonData.isRetry = Boolean.parseBoolean(prop("isrerun"));
    commonData.setExecutionID = Boolean.parseBoolean(prop("setExecutionID"));
    if (prop("buildRef") != null) {
      TestraRestClient.buildRef = prop("buildRef");
    }
    if (prop("testra.execution.description") != null) {
      TestraRestClient.executionDescription = prop("testra.execution.description");
    }
    createExecution();
    if (commonData.isRetry) {
      List<EnrichedTestResult> failedTests = TestraRestClient.getFailedResults();
      failedTests.forEach(x -> {
        commonData.failedScenarioIDs.put(x.getTargetId(), x.getId());
        commonData.failedRetryMap.put(x.getTargetId(), x.getRetryCount());
      });
    }
  }

  private void snippetHandler(SnippetsSuggestedEvent event) {
    commonData.snippetLine.add(event.snippets.get(0));
  }

  private void handleFeatureStartedHandler(final TestSourceRead event) {
    cucumberSourceUtils.addTestSourceReadEvent(event.uri, event);
    commonData.cucumberSourceUtils.addTestSourceReadEvent(event.uri, event);
    processBackgroundSteps(commonData.cucumberSourceUtils.getFeature(event.uri),
        MD5.generateMD5(event.uri));
  }

  private synchronized void createExecution() {
    if (TestraRestClient.getExecutionid() == null) {
      if (commonData.isRetry) {
        File file = new File("testra.exec");
        if (file.isFile()) {
          try {
            TestraRestClient.setExecutionid(Files.readFile(file).trim());
          } catch (IOException e) {
            e.printStackTrace();
          }
        } else {
          TestraRestClient.setExecutionid(prop("previousexecutionID"));
        }
      } else if (commonData.setExecutionID) {
        TestraRestClient.setExecutionid(prop("previousexecutionID"));
      } else {
        TestraRestClient.setExecutionid(null);
        createExecutionIDFile();
      }
    }
    executionID = TestraRestClient.getExecutionid();
    LOGGER.info("Execution ID is: " + executionID);
    LOGGER.info(prop("host") + "/projects/" + projectID + "/executions/" + executionID);
  }

  private void createExecutionIDFile() {
    File file = new File("testra.exec");
    FileWriter writer = null;
    try {
      if (file.createNewFile()) {
        System.out.println("File is created!");
      } else {
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

  private void processBackgroundSteps(Feature feature, String featureHash) {
    List<StepTemplate> backgroundSteps = new ArrayList<>();
    List<String> featureTags = new ArrayList<>();
    feature.getTags().forEach(x -> featureTags.add(x.getName()));
    ScenarioDefinition background = feature.getChildren().stream()
        .filter(s -> s.getClass().getSimpleName().equals("Background"))
        .findFirst()
        .orElse(null);

    if (background != null) {
      int counter = 0;
      for (Step step : background.getSteps()) {
        StepTemplate stepTemplate = new StepTemplate();
        if (step.getArgument() != null) {
          DataTable datatable;
          Node argument = step.getArgument();
          if (argument instanceof DataTable) {
            datatable = (DataTable) argument;
            List<DataTableRow> dtrRows = new ArrayList<>();
            int rowcounter = 0;
            for (TableRow tableRow : datatable.getRows()) {
              DataTableRow dtr = new DataTableRow();
              dtr.setIndex(rowcounter);
              int cellCounter = 0;
              for (TableCell tableCell : tableRow.getCells()) {
                DataTableCell dtc = new DataTableCell();
                dtc.setIndex(cellCounter);
                dtc.setValue(tableCell.getValue());
                cellCounter++;
                dtr.addCellsItem(dtc);
              }
              dtrRows.add(dtr);
              rowcounter++;
            }
            stepTemplate.setDataTableRowList(dtrRows);
          }
        }

        stepTemplate.setGherkinStep(step.getKeyword() + step.getText());
        stepTemplate.setIndex(counter);
        stepTemplate.setLine(step.getLocation().getLine());
        counter++;
        backgroundSteps.add(stepTemplate);
      }
    }
    commonData.backgroundSteps.put(featureHash, backgroundSteps);
  }


  private void handleTestCaseStarted(final TestCaseStarted event) {
    commonData.startTime = System.currentTimeMillis();
    commonData.currentFeatureFile = event.testCase.getUri();
    commonData.currentFeature = commonData.cucumberSourceUtils
        .getFeature(commonData.currentFeatureFile);
    commonData.currentTestCase = event.testCase;
    final List<String> tagList = new ArrayList<>();
    event.testCase.getTags().forEach(x -> tagList.add(x.getName()));
    ScenarioRequest scenarioRequest = new tech.testra.java.client.model.ScenarioRequest();
    scenarioRequest.setName(event.testCase.getName());
    scenarioRequest.setFeatureName(commonData.currentFeature.getName());
    scenarioRequest.setFeatureDescription(commonData.currentFeature.getDescription());
    scenarioRequest.setTags(tagList);
    if (tagList.contains("@Manual")) {
      scenarioRequest.setManual(true);
      commonData.isManual = true;
    } else {
      commonData.isManual = false;
    }
    scenarioRequest.setBackgroundSteps(
        commonData.backgroundSteps.get(MD5.generateMD5(event.testCase.getUri())).stream()
            .map(s -> {
              TestStep testStep = new TestStep();
              testStep.setIndex(s.getIndex());
              testStep.setText(s.getGherkinStep());
              testStep.setDataTableRows(s.getDataTableRowList());
              return testStep;
            }).collect(Collectors.toList()));
    List<TestStep> testStepList = new ArrayList<>();
    List<PickleStepTestStep> pickleSteps = commonData.currentTestCase.getTestSteps().stream()
        .filter(x -> !x.isHook())
        .map(x -> (PickleStepTestStep) x)
        .collect(Collectors.toList());
    for (int i = commonData.backgroundSteps.get(MD5.generateMD5(event.testCase.getUri())).size();
        i < pickleSteps.size(); i++) {
      TestStep testStep = new TestStep();
      testStep.setIndex(i);
      PickleStepTestStep pickleStep = pickleSteps.get(i);
      if (pickleStep.getStepArgument().size() > 0) {
        PickleTable pickleTable = ((PickleTable) pickleStep.getStepArgument().get(0));
        List<DataTableRow> dtrRows = new ArrayList<>();
        int rowcounter = 0;
        for (PickleRow pickleRow : pickleTable.getRows()) {
          DataTableRow dtr = new DataTableRow();
          dtr.setIndex(rowcounter);
          int cellcounter = 0;
          for (PickleCell pickleCell : pickleRow.getCells()) {
            DataTableCell dtc = new DataTableCell();
            dtc.setIndex(cellcounter);
            cellcounter++;
            dtc.setValue(pickleCell.getValue());
            dtr.addCellsItem(dtc);
          }
          dtrRows.add(dtr);
          rowcounter++;
        }
        testStep.setDataTableRows(dtrRows);
      }
      testStep.setText(commonData.cucumberSourceUtils
          .getKeywordFromSource(event.testCase.getUri(), pickleSteps.get(i).getStepLine())
          + ((pickleSteps.get(i))).getStepText());
      testStepList.add(testStep);
    }
    if (tagList.contains("@ExpectedFailure")) {
      commonData.isExpectedFailure = true;
    } else {
      commonData.isExpectedFailure = false;
    }
    scenarioRequest.setSteps(testStepList);
    Scenario scenario = TestraRestClient.createScenario(scenarioRequest);
    commonData.currentScenarioID = scenario.getId();
    commonData.currentFeatureID = scenario.getFeatureId();

    if (commonData.isRetry && !commonData.failedScenarioIDs
        .containsKey(commonData.currentScenarioID)) {
      LOGGER.info("Test has already passed in a previous test run");
      if (prop("junit") == null || !Boolean.parseBoolean(prop("junit"))) {
        throw new SkipException("Test already passed, skipping");
      }
    } else if (commonData.isRetry) {
      commonData.currentTestResultID = commonData.failedScenarioIDs
          .get(commonData.currentScenarioID);
      commonData.retryCount = commonData.failedRetryMap.get(commonData.currentScenarioID) + 1;
    }

  }

  private void handleTestStepStarted(final TestStepStarted event) {
  }

  private void handleTestStepFinished(final TestStepFinished event) {
    if (commonData.skip) {
      return;
    }
    if (event.testStep.isHook()) {
      handleHookStep(event);
    } else {
      handlePickleStep(event);
    }
  }

  private void handleHookStep(final TestStepFinished event) {
  }

  private void handleTestCaseFinished(final TestCaseFinished event) {
    if (commonData.skip) {
      commonData.skip = false;
      commonData.stepResultsNew = new ArrayList<>();
      commonData.embedEvent = null;
      return;
    }
    if (commonData.isManual) {
      TestResultRequest testResultRequest = new TestResultRequest();
      testResultRequest.setGroupId(commonData.currentFeatureID);
      testResultRequest.setTargetId(commonData.currentScenarioID);
      testResultRequest.setDurationInMs(0L);
      testResultRequest.setResultType(ResultTypeEnum.SCENARIO);
      testResultRequest.setStatus(StatusEnum.SKIPPED);
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
    testResultRequest.setDurationInMs(event.result.getDuration());
    testResultRequest.setResultType(ResultTypeEnum.SCENARIO);
    testResultRequest.setStatus(StatusEnum.fromValue(event.result.getStatus().toString()));
    testResultRequest.setStepResults(commonData.stepResultsNew);
    testResultRequest.setStartTime(commonData.startTime);
    testResultRequest.setEndTime(commonData.endTime);
    testResultRequest.setRetryCount(commonData.retryCount);
    testResultRequest.setExpectedToFail(commonData.isExpectedFailure);
    commonData.isExpectedFailure = false;
    if (event.result.getStatus().equals(Type.FAILED)) {
      testResultRequest.setError(event.result.getErrorMessage());
      if (commonData.embedEvent != null) {
        Attachment attachment = new Attachment();
        attachment.setMimeType(commonData.embedEvent.mimeType);
        attachment.setBase64EncodedByteArray(
            new String(Base64.getEncoder().encode(commonData.embedEvent.data)));
        testResultRequest
            .setAttachments(Collections.singletonList(attachment));
      }
    }

    commonData.embedEvent = null;
    commonData.stepResultsNew = new ArrayList<>();
    if (commonData.isRetry) {
      commonData.resultID = TestraRestClient
          .updateResult(commonData.currentTestResultID, testResultRequest).getId();
    } else
      commonData.resultID = TestraRestClient.createResult(testResultRequest).getId();
  }

  private void handlePickleStep(final TestStepFinished event) {
    cucumber.api.TestStep testStep = event.testStep;
    StepResult stepResult = new StepResult();
    stepResult.setDurationInMs(event.result.getDuration());
    stepResult.setIndex(commonData.resultCounter);
    commonData.resultCounter = commonData.resultCounter + 1;
    stepResult.setStatus(StepResult.StatusEnum.fromValue(event.result.getStatus().toString()));
    if (event.result.getStatus().equals(Type.UNDEFINED)) {
      if (commonData.snippetLine.size() > 0) {
        stepResult.setError(commonData.snippetLine.get(commonData.snippetCount));
        commonData.snippetCount++;
      } else {
        stepResult.setError(
            "No step found for scenario line: " + testStep.getPickleStep().getText());
      }
    }
    if (event.result.getStatus().equals(Type.FAILED)) {
      stepResult.setError(event.result.getErrorMessage());
    }
    commonData.stepResultsNew.add(stepResult);
  }

}
