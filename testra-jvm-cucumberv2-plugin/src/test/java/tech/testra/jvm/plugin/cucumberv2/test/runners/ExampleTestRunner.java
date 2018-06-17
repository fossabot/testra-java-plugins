package tech.testra.jvm.plugin.cucumberv2.test.runners;

import cucumber.api.CucumberOptions;
import cucumber.api.testng.AbstractTestNGCucumberTests;

/**
 * A sample test to demonstrate
 */
@CucumberOptions(
    tags = "@example",
    features = "src/test/resources/feature_files",
    glue = {""},
    plugin = {"com.williamhill.whgtf.testra.jvm.pluginv2.TestraCucumberJvmV2:mytest.properties"
    }
)
public class ExampleTestRunner extends AbstractTestNGCucumberTests {

}