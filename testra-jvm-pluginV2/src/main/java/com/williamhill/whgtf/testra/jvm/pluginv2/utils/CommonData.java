package com.williamhill.whgtf.testra.jvm.pluginv2.utils;

import com.williamhill.whgtf.test.bnw.pojo.testresult.StepResult;
import com.williamhill.whgtf.testra.jvm.client.api.TestraRestClient;
import com.williamhill.whgtf.testra.jvm.pluginv2.StepTemplate;
import cucumber.api.TestCase;
import gherkin.ast.Feature;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommonData {
  private static final Logger LOGGER = LoggerFactory.getLogger(CommonData.class);
  private CucumberSourceUtils cucumberSourceUtils;
  private Feature currentFeature;
  private String currentFeatureFile;
  private TestCase currentTestCase;
  private static TestraRestClient testraRestClient;
  private List<StepTemplate> backgroundSteps;
  private List<StepResult> stepResults;
  private final String TYPE_SCENARIO = "SCENARIO";
  private String currentScenarioID;
  private byte[] screenShot;
  private boolean isScreenshot;

  public CommonData(){

  }


  public TestraRestClient getTestraRestClient() {
    if (testraRestClient == null) {
      testraRestClient = new TestraRestClient();
    }
    return testraRestClient;
  }

  public void setScreenShot(byte[] screenshot){
    screenShot = screenshot;
    isScreenshot = true;
  }

  public CucumberSourceUtils getCucumberSourceUtils(){
    if(cucumberSourceUtils == null){
      cucumberSourceUtils = new CucumberSourceUtils();
    }
    return cucumberSourceUtils;
  }


  public List<StepTemplate> getBackgroundSteps() {
    if(backgroundSteps==null){
      backgroundSteps = new ArrayList<>();
    }
    return backgroundSteps;
  }

  public void setBackgroundSteps(
      List<StepTemplate> backgroundSteps) {
    this.backgroundSteps = backgroundSteps;
  }

  public Feature getCurrentFeature(){
    return currentFeature;
  }

  public void setCurrentFeature(Feature feature){
    currentFeature = feature;
  }

  public String getCurrentFeatureFile(){
    return currentFeatureFile;
  }

  public void setCurrentFeatureFile(String featureFile){
    currentFeatureFile = featureFile;
  }

  public TestCase getCurrentTestCase() {
    return currentTestCase;
  }

  public void setCurrentTestCase(TestCase currentTestCase) {
    this.currentTestCase = currentTestCase;
  }

  public String getCurrentScenarioID() {
    return currentScenarioID;
  }

  public void setCurrentScenarioID(String currentScenarioID) {
    this.currentScenarioID = currentScenarioID;
  }

  public List<StepResult> getStepResults() {
    return stepResults;
  }

  public void setStepResults(
      List<StepResult> stepResults) {
    this.stepResults = stepResults;
  }

  public boolean isScreenshot() {
    return isScreenshot;
  }

  public void setScreenshot(boolean screenshot) {
    isScreenshot = screenshot;
  }

  public byte[] getScreenShot() {
    return screenShot;
  }
}
