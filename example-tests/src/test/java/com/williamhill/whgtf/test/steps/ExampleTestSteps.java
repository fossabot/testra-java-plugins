package com.williamhill.whgtf.test.steps;

import com.williamhill.whgtf.testra.jvm.client.api.TestraRestClient;
import cucumber.api.PendingException;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

import static com.williamhill.whgtf.testra.jvm.util.PropertyHelper.prop;
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
