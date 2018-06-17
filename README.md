# testra-cucumber-jvm-plugin
Plugin for Cucumber JVM

## Getting Started
Add the Testra plugin to your TestNG runner
```$xslt
@CucumberOptions(
    tags = "@selenium",
    features = "testra-jvm-pluginV2/src/test/resources/feature_files",
    glue = {""},
    plugin = {"tech.testra.jvm.plugin.cucumberv2.Testra"
    }
)
public class ExampleSeleniumRunner extends AbstractTestNGCucumberTests {

}
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
In your @After teardown function add a screenshot to Testra reports
```$xslt
    byte[] screenshot = ((TakesScreenshot) webDriver).getScreenshotAs(OutputType.BYTES);
    Testra.setScreenShot(screenshot);
```
