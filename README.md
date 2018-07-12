# Testra Java Plugin

This is the test plugin including API Client for Testra.
It includes plugins for cucumber versions 1.2.5, 2.4.0 and 3.0.2

## Table of Contents

- [Install](#install)
- [Cucumber Usage](#cucumber)
- [Junit Usage](#junit)
- [Properties](#properties)
- [Tags](#tags)
- [Contribute](#contribute)
- [License](#license)


## Install

Add the following dependency group to your build.gradle file
```
dependency group: 'tech.testra', name: 'testra-java-cucumberv2-plugin', version: "${testraPluginVersion}"
```
Replacing v2 with the version of cucumber you are using 1,2 or 3

## Cucumber
Either add the plugin as a command line argument:
```
-Dcucumber.options="--plugin tech.testra.jvm.plugin.cucumberv2.Testra"
```
Or to the cucumber options of your runner (TestNG)
```$xslt
@CucumberOptions(
    tags = "@example",
    features = "src/test/resources/feature_files",
    glue = {""},
    plugin = {"tech.testra.jvm.plugin.cucumberv2.Testra"}
)
public class ExampleTestRunner extends AbstractTestNGCucumberTests {
}
```

Or to the cucumber options of your runner (Junit)
```$xslt
@RunWith(Cucumber.class)
@CucumberOptions(
    tags = "@example",
    features = "src/test/resources/feature_files",
    glue = {""},
    plugin = {
        "tech.testra.jvm.plugin.cucumberv2.Testra"
    }
)
public class ExampleTestRunnerJunit {

}
```

If you are using junit and wish to use the retry functionality to rerun failed tests you must add the following to your test steps @Before
```$xslt
  @Before
  public void setup(){
    TestraJunitHelper junitHelper = new TestraJunitHelper();
    junitHelper.checkSkip();
  }
```
And you must have junit=true as a jvm arg or in .testra



## Junit
To run with Junit4, run with the testra junit runner
```$xslt
@RunWith(TestraJunitRunner.class)
```
Use the @Tag annotation to add tags to both the class and tests.


##Properties
You must have a .testra file in the root directory for the module which defines
```$xslt
host=http://localhost:8080/api/v1
project=First Test Project
branch=develop
testra.environment=pp2
debug=true
buildRef=ref
testra.execution.description=Run for major release
isrerun=false
setExecutionID=false
previousexecutionID=5b34b3b5c83c6c2f86b65e99
junit=false
```
If isrerun is set to true, set the previousexecutionID to the last execution id and the plugin will skipp all previously passed scenarios.
Note any of these properties can be set as jvm arguments.

Each time you complete a test run, a testra.exec file will be created with the previous execution id.
If this file is present, there is no need to add previousexecutionID to the .testra file.

Run testra.disabled=true as a jvm argument to disable the Testra Plugin

Any .testra properties that are passed as jvm args, the jvm arg will be used first.

##Tags
Adding @ExpectedFailure as a tag to a test will add the 'expectedtofail' flag to the test results for that test. In the testra user interface this will allow you to filter out expected failures from your reports.

Adding @Manual as a tag to a test will add the manual flag to a scenario and will skip the test to be done manually at the end.
## Contribute


PRs accepted.


## License

[MIT](https://github.com/nishanths/license/blob/master/LICENSE)