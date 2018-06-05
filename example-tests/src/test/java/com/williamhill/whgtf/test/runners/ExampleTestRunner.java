package com.williamhill.whgtf.test.runners;

import com.cucumber.listener.Reporter;
import cucumber.api.CucumberOptions;
import cucumber.api.testng.AbstractTestNGCucumberTests;
import java.io.File;
import org.junit.AfterClass;

@CucumberOptions(
    plugin = {"pretty",
        "json:build/cucumber/cucumber-json-example.json",
        "com.cucumber.listener.ExtentCucumberFormatter:target/cucumber-reports/report.html"
    },
    monochrome = true,
    tags = "@example",
    features = "example-tests/src/test/resources/feature_files",
    glue = {""})
public class ExampleTestRunner extends AbstractTestNGCucumberTests {
    @AfterClass
    public static void writeExtentReport(){
        Reporter.loadXMLConfig(new File("example-tests/src/test/resources/extent-config.xml"));
        Reporter.setSystemInfo("user", System.getProperty("user.name"));
        Reporter.setSystemInfo("os", "Mac OSX");
        Reporter.setTestRunnerOutput("Sample test runner output message");
    }
}