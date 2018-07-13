package tech.testra.jvm.plugin.cucumberv2.test.unit;

import cucumber.runtime.Argument;
import cucumber.runtime.ParameterInfo;
import cucumber.runtime.StepDefinition;
import gherkin.pickles.PickleStep;
import java.lang.reflect.Type;
import java.util.List;

class JavaStepDefinition implements StepDefinition {


  public JavaStepDefinition() {

  }

  @Override
  public List<Argument> matchedArguments(PickleStep step) {
    return null;
  }

  @Override
  public String getLocation(boolean detail) {
    return null;
  }

  @Override
  public Integer getParameterCount() {
    return null;
  }

  @Override
  public ParameterInfo getParameterType(int n, Type argumentType) throws IndexOutOfBoundsException {
    return null;
  }

  @Override
  public void execute(String language, Object[] args) throws Throwable {

  }

  @Override
  public boolean isDefinedAt(StackTraceElement stackTraceElement) {
    return false;
  }

  @Override
  public String getPattern() {
    return null;
  }

  @Override
  public boolean isScenarioScoped() {
    return false;
  }
}