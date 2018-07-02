package tech.testra.jvm.plugin.cucumberv2.utils;

import cucumber.api.TestCase;
import cucumber.api.event.EmbedEvent;
import gherkin.ast.Feature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.testra.jvm.plugin.cucumberv2.StepTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommonData {
  private static final Logger LOGGER = LoggerFactory.getLogger(CommonData.class);
  public CucumberSourceUtils cucumberSourceUtils = new CucumberSourceUtils();
  public Feature currentFeature;
  public String currentFeatureFile;
  public TestCase currentTestCase;
  public Map<String,List<StepTemplate>> backgroundSteps = new HashMap<>();
  public List<tech.testra.java.client.model.StepResult> stepResultsNew = new ArrayList<>();
  public final String TYPE_SCENARIO = "SCENARIO";
  public String currentScenarioID;
  public String currentFeatureID;
  public EmbedEvent embedEvent;
  public List<String> snippetLine = new ArrayList<>();
  public int snippetCount = 0;
  public long startTime;
  public long endTime;
  public int retryCount = 0;
  public Boolean isRetry;
  public Boolean setExecutionID;
  public String currentTestResultID;
  public Map<String,String> failedScenarioIDs = new HashMap<>();
  public Map<String,Integer> failedRetryMap = new HashMap<>();


  }