package com.williamhill.whgtf.test.runners;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import org.junit.runner.RunWith;

/**
 * A sample test to demonstrate
 */
@RunWith(Cucumber.class)
@CucumberOptions(
    tags = "@example",
    features = "example-tests/src/test/resources/feature_files",
    glue = {""},
    plugin = {"pretty",
        "json:build/cucumber/cucumber-json-example.json",
        "com.williamhill.whgtf.testra.jvm.plugin.TestraCucumberJvm"
    }
)
public class ExampleTestRunner {

}