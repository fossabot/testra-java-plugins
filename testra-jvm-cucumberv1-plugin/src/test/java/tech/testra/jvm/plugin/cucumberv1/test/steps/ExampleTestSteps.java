package tech.testra.jvm.plugin.cucumberv1.test.steps;

import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;

import static org.assertj.core.api.Assertions.assertThat;


public class ExampleTestSteps {

  @Given("^(\\d+) plus (\\d+) equals (\\d+)$")
  public void additionTester(Integer arg0, Integer arg1, Integer arg2){
    assertThat(arg0+arg1)
        .as("Error args " + arg0 + " + " + arg1 + " should equal " + arg2 + " but was " + (arg0+arg1))
        .isEqualTo(arg2);
  }

  @Then("^(\\d+) minus (\\d+) equals (\\d+)$")
  public void minusEquals(int arg0, int arg1, int arg2){
    assertThat(arg0-arg1)
        .as("Error args " + arg0 + " - " + arg1 + " should equal " + arg2 + " but was " + (arg0-arg1))
        .isEqualTo(arg2);
  }

  @Then("^(\\d+) plus (\\d+) does not equal (\\d+)$")
  public void plusDoesNotEqual(int arg0, int arg1, int arg2) throws Throwable {
    assertThat(arg0+arg1)
        .as("Error args " + arg0 + " + " + arg1 + " should not equal " + arg2 + " but was " + (arg0+arg1))
        .isNotEqualTo(arg2);
  }


}
