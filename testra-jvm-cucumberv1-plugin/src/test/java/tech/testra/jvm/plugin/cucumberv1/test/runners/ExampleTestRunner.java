package tech.testra.jvm.plugin.cucumberv1.test.runners;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import org.junit.runner.RunWith;

/**
 * A sample test to demonstrate
 */
@RunWith(Cucumber.class)
@CucumberOptions(
    tags = "@example",
    features = "src/test/resources/feature_files",
    glue = {""},
    plugin = {
        "tech.testra.jvm.plugin.cucumberv1.Testra"
    }
)
public class ExampleTestRunner {

}