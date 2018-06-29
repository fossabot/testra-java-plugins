# Testra Java Plugin

![banner]()

![badge]()
![badge]()


This is the test plugin including API Client for Testra.
It includes plugins for both cukes cucumber version 1 and io.cucumber version 2.

## Table of Contents

- [Background](#background)
- [Install](#install)
- [Usage](#usage)
- [Contribute](#contribute)
- [License](#license)

## Security

### Any optional sections

## Background

### Any optional sections

## Install

Add the following dependency groups to your build.gradle file (swapping v2 for v1 if you're using cukes cucumber v1)
```
dependency group: 'tech.testra', name: 'testra-java-cucumberv2-plugin', version: "${testraPluginVersion}"
dependency group: 'tech.testra', name: 'testra-java-api-client', version: "${testraPluginVersion}"
dependency group: 'tech.testra', name: 'testra-java-commons', version: "${testraPluginVersion}"
```


## Usage
Either add the plugin as a command line argument:
```
-Dcucumber.options="--plugin tech.testra.jvm.plugin.cucumberv2.Testra"
```
Or to the cucumber options of your runner
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
```
If isrerun is set to true, set the previousexecutionID to the last execution id and the plugin will skipp all previously passed scenarios.
Note any of these properties can be set as jvm arguments.


## Contribute


PRs accepted.


## License

[MIT](https://github.com/nishanths/license/blob/master/LICENSE)