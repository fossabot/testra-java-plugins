package tech.testra.jvm.plugin.cucumberv2.utils;

import cucumber.api.TestCase;
import cucumber.api.event.EmbedEvent;
import gherkin.ast.Feature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.testra.jvm.api.client.api.TestraRestClient;
import tech.testra.jvm.plugin.cucumberv2.StepTemplate;

import java.util.ArrayList;
import java.util.List;

public class CommonData {
  private static final Logger LOGGER = LoggerFactory.getLogger(CommonData.class);
  public CucumberSourceUtils cucumberSourceUtils = new CucumberSourceUtils();
  public Feature currentFeature;
  public String currentFeatureFile;
  public TestCase currentTestCase;
  private static TestraRestClient testraRestClient;
  public List<StepTemplate> backgroundSteps = new ArrayList<>();
  public List<tech.testra.jvm.client.model.StepResult> stepResultsNew = new ArrayList<>();
  public final String TYPE_SCENARIO = "SCENARIO";
  public String currentScenarioID;
  public EmbedEvent embedEvent;

  public CommonData() {
  }


  public TestraRestClient getTestraRestClient() {
    if (testraRestClient == null) {
      testraRestClient = new TestraRestClient();
    }
    return testraRestClient;
  }
}