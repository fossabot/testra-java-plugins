# testra-cucumber-jvm-plugin
Plugin for Cucumber JVM

## Getting Started
Add the TestraCucumberJvmV2 plugin to your TestNG runner
```$xslt
@CucumberOptions(
    tags = "@selenium",
    features = "testra-jvm-pluginV2/src/test/resources/feature_files",
    glue = {""},
    plugin = {"com.williamhill.whgtf.testra.jvm.pluginv2.TestraCucumberJvmV2"
    }
)
public class ExampleSeleniumRunner extends AbstractTestNGCucumberTests {

}
```

in local.environment.properties enter the url of the Testra server

## Enable Screenshots
In your @After teardown function add a screenshot to Testra reports
```$xslt
    byte[] screenshot = ((TakesScreenshot) webDriver).getScreenshotAs(OutputType.BYTES);
    TestraCucumberJvmV2.setScreenShot(screenshot);
```
