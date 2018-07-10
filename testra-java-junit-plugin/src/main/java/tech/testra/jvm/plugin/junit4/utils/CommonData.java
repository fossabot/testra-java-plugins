package tech.testra.jvm.plugin.junit4.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommonData {
  public String currentTestCaseID;
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
  }