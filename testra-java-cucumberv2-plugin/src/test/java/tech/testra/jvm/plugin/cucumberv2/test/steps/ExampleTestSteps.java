package tech.testra.jvm.plugin.cucumberv2.test.steps;

import cucumber.api.DataTable;
import cucumber.api.PendingException;
import cucumber.api.java.Before;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import tech.testra.jvm.plugin.cucumberv2.TestraJunitHelper;

import static org.assertj.core.api.Assertions.assertThat;


public class ExampleTestSteps {

  @Before
  public void setup(){
    TestraJunitHelper junitHelper = new TestraJunitHelper();
    junitHelper.checkSkip();
  }

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
  public void plusDoesNotEqual(int arg0, int arg1, int arg2) {
    assertThat(arg0+arg1)
        .as("Error args " + arg0 + " + " + arg1 + " should not equal " + arg2 + " but was " + (arg0+arg1))
        .isNotEqualTo(arg2);
  }


  @And("^(.*) is true$")
  public void trueIsTrue(boolean var){
    assertThat(var);
  }

  @And("^dataTable is readable (.*)$")
  public void datatableIsReadable(String word, DataTable dataTable) throws Throwable {
    assertThat(dataTable.cells(0).size() >0).isTrue();
  }
}
