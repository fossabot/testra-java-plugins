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
  public CucumberSourceUtils cucumberSourceUtils = new CucumberSourceUtils();
  public Feature currentFeature;
  public String currentFeatureFile;
  public TestCase currentTestCase;
  private static TestraRestClient testraRestClient;
  public List<StepTemplate> backgroundSteps = new ArrayList<>();
  public List<StepResult> stepResults = new ArrayList<>();
  public final String TYPE_SCENARIO = "SCENARIO";
  public String currentScenarioID;
  public static byte[] screenShot;
  public static boolean isScreenshot;

  public CommonData(){

  }


  public TestraRestClient getTestraRestClient() {
    if (testraRestClient == null) {
      testraRestClient = new TestraRestClient();
    }
    return testraRestClient;
  }

  public void setScreenShot(byte[] screenshot){
    if(screenshot == null) {
      screenShot = null;
      isScreenshot = false;
    }
    else{
      screenShot = screenshot;
      isScreenshot = true;
    }
  }

}
