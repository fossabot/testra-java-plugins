# Testra Java Plugin

This is the test plugin including API Client for Testra.
It includes plugins for cucumber versions 1.2.5, 2.4.0 and 3.0.2

## Table of Contents

- [Install](#install)
- [Cucumber Usage](#cucumber)
- [Junit Usage](#junit)
- [Spock Usage](#spock)
- [Properties](#properties)
- [Screenshots](#screenshots)
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

To run with Junit5, use a LauncherFactory similar to:
```$xslt
    private void runClasses(Class<?>... classes) {
        final ClassSelector[] classSelectors = Stream.of(classes)
            .map(DiscoverySelectors::selectClass)
            .toArray(ClassSelector[]::new);
        final LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
            .selectors(classSelectors)
            .build();

        final Launcher launcher = LauncherFactory.create();
        launcher.execute(request, new Testra());
    }
```

## Spock
To run with Spock, use a spock runner
```$xslt
    private final static NOTIFIER = new RunNotifier()
    static void run(Class clazz){
        SpecInfo spec = new SpecInfoBuilder(clazz).build()
        spec.addListener(new Testra())
        new JUnitDescriptionGenerator(spec).describeSpecMethods()
        new JUnitDescriptionGenerator(spec).describeSpec()
        RunContext.get().createSpecRunner(spec, NOTIFIER).run()
    } 
```
Then run the tests with Junit
```$xslt
    @Test
    void shouldStartTest(){
        TestraRunner.run(TestGroovy)
    }
```
Where TestGroovy is the name of your Spock class

## Properties
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
If isrerun is set to true, set the previousexecutionID to the last execution id and the plugin will skip all previously passed scenarios.
Note any of these properties can be set as jvm arguments.

Each time you complete a test run, a testra.exec file will be created with the previous execution id.
If this file is present, there is no need to add previousexecutionID to the .testra file.

Run testra.disabled=true as a jvm argument to disable the Testra Plugin

Any .testra properties that are passed as jvm args, the jvm arg will be used first.

##Screenshots
To take a screenshot with selenium, add the following to your @After
```#xslt
  @After
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

The simplest way to make screenshots to work with selenide is to do the same but import static com.codeborne.selenide.WebDriverRunner.getWebDriver
```$xslt
    if(getWebDriver()!=null){
      try {
        if (scenario.isFailed()) {
          byte[] screenshot = ((TakesScreenshot) getWebDriver()).getScreenshotAs(OutputType.BYTES);
          scenario.embed(screenshot, "image/png");
        }
      } catch (WebDriverException | ClassCastException somePlatformsDontSupportScreenshots) {
        somePlatformsDontSupportScreenshots.printStackTrace();
      }
    }
``` 

##Tags
Adding @ExpectedFailure as a tag to a test will add the 'expectedtofail' flag to the test results for that test. In the testra user interface this will allow you to filter out expected failures from your reports.

Adding @Manual as a tag to a test will add the manual flag to a scenario and will skip the test to be done manually at the end.
## Contribute


PRs accepted.


## License

[GNu GPLv3](https://github.com/testra-tech/testra-java-plugins/blob/master/LICENSE)
