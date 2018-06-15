package test.runners;

import cucumber.api.CucumberOptions;
import cucumber.api.java.After;
import cucumber.api.testng.AbstractTestNGCucumberTests;

/**
 * A sample test to demonstrate
 */
@CucumberOptions(
    tags = "@selenium",
    features = "src/test/resources/feature_files",
    glue = {""},
    plugin = {"com.williamhill.whgtf.testra.jvm.pluginv2.TestraCucumberJvmV2"
    }
)
public class ExampleSeleniumRunner extends AbstractTestNGCucumberTests {

}