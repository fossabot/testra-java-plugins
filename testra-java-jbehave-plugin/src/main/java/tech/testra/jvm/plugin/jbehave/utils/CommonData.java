package tech.testra.jvm.plugin.jbehave.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommonData {
  private static final Logger LOGGER = LoggerFactory.getLogger(CommonData.class);
  public String currentFeatureFile;
  public List<tech.testra.java.client.model.StepResult> stepResultsNew = new ArrayList<>();
  public final String TYPE_SCENARIO = "SCENARIO";
  public String currentScenarioID;
  public String currentFeatureID;
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
  public int resultCounter = 0;
  public boolean skip = false;
  public boolean isManual = false;
  public boolean isExpectedFailure = false;
  public String resultID;
  public long stepStartTime;
  public boolean isFailed = false;
  public String failureMessage;
  public boolean isSkipped = false;
  public boolean isPending = false;
  }