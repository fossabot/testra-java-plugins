package tech.testra.jvm.plugin.junit5.utils;

import java.util.HashMap;
import java.util.Map;

public class CommonData {
  public String currentTestCaseID;
  public String currentGroupID;
  public long startTime;
  public long endTime;
  public Boolean isRetry;
  public Boolean setExecutionID;
  public Map<String,String> failedScenarioIDs = new HashMap<>();
  public Map<String,Integer> failedRetryMap = new HashMap<>();
  public int resultCounter = 0;
  public boolean skip = false;
  public boolean isDisabled = false;
  public String failureMessage = "";
  public boolean isFailure = false;
  public boolean isIgnored = false;
  public boolean isManual = false;
  public boolean isExpectedFailure = false;
  }