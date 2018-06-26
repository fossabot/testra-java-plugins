package tech.testra.jvm.plugin.cucumberv1.utils;

import gherkin.formatter.model.Feature;
import gherkin.formatter.model.Result;
import gherkin.formatter.model.Scenario;
import gherkin.formatter.model.Step;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.testra.jvm.plugin.cucumberv1.templates.ScenarioTemplate;

public class CommonData {
  private static final Logger LOGGER = LoggerFactory.getLogger(CommonData.class);

  public Feature currentFeature;
  public String currentFeatureFile;
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

}