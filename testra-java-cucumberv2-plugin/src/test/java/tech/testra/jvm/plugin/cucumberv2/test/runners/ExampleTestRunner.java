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
    plugin = {"tech.testra.jvm.plugin.cucumberv2.Testra:.testra"
    }
)
public class ExampleTestRunner extends AbstractTestNGCucumberTests {
}