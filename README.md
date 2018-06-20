# testra-cucumber-jvm-plugin
Plugin for Cucumber JVM

## Getting Started
####TestNG Cucumber
Add the Testra v2 plugin to your TestNG runner
```$xslt
@CucumberOptions(
    tags = "@selenium",
    features = "src/test/resources/feature_files",
    glue = {""},
    plugin = {"tech.testra.jvm.plugin.cucumberv2.Testra:myproperties.properties"
    }
)
public class ExampleSeleniumRunner extends AbstractTestNGCucumberTests {

}
```
####Junit cucumber
Add the Testra v1 plugin to your Junit runner
```$xslt
@RunWith(Cucumber.class)
@CucumberOptions(
    tags = "@example",
    features = "src/test/resources/feature_files",
    glue = {""},
    plugin = {
        "tech.testra.jvm.plugin.cucumberv1.Testra:myproperties.properties"
    }
)
```

Passing in the property file as a parameter (myproperties.properties in the example above)

###Property File
The property file must have the following two properties
```$xslt
host=http://localhost:8080/api/v1
project=My Test Project
```
Where host is the testra host url

## Enable Screenshots
In your @After teardown function add a screenshot to Testra reports using inbuild scenario embed
```$xslt
  public void teardown(Scenario scenario){
    if(webDriver!=null){
      try {
        if (scenario.isFailed()) {
          byte[] screenshot = ((TakesScreenshot) webDriver).getScreenshotAs(OutputType.BYTES);
          scenario.embed(screenshot, "image/png");
        }
      } catch (WebDriverException | ClassCastException somePlatformsDontSupportScreenshots) {
        somePlatformsDontSupportScreenshots.printStackTrace();
      }
      webDriver.close();
    }
  }
```
