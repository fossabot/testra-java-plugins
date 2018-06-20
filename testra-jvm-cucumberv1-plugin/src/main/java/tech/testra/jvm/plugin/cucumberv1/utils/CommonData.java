package tech.testra.jvm.plugin.cucumberv1.utils;

import gherkin.formatter.model.Feature;
import gherkin.formatter.model.Result;
import gherkin.formatter.model.Scenario;
import gherkin.formatter.model.Step;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.testra.jvm.apiv2.client.api.TestraRestClientV2;
import tech.testra.jvm.plugin.cucumberv1.templates.ScenarioTemplate;

public class CommonData {
  private static final Logger LOGGER = LoggerFactory.getLogger(CommonData.class);

  public Feature currentFeature;
  public String currentFeatureFile;
  private static TestraRestClientV2 testraRestClientV2;
  public List<tech.testra.jvm.client.model.StepResult> stepResultsNew = new ArrayList<>();
  public final String TYPE_SCENARIO = "SCENARIO";
  public String attachmentMimeType;
  public byte[] attachmentByteArray;
  public String currentScenarioID;
  public Scenario currentScenario;
  public long startTime;
  public boolean isScenarioOutline = false;
  public int scenarioOutlineIndex;
  public String ExecutionID;
  public Step currentStep;
  public ScenarioTemplate currentScenarioTemplate;
  public int stepIndex = 0;
  public Result result;

  public CommonData() {
  }


//  public TestraRestClient getTestraRestClient() {
//    if (testraRestClient == null) {
//      testraRestClient = new TestraRestClient();
//    }
//    return testraRestClient;
//  }

  public TestraRestClientV2 getTestraRestClientV2() {
    if (testraRestClientV2 == null) {
      testraRestClientV2 = new TestraRestClientV2();
    }
    return testraRestClientV2;
  }
}