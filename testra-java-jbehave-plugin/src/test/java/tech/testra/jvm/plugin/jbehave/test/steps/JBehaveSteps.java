package tech.testra.jvm.plugin.jbehave.test.steps;

import static org.assertj.core.api.Assertions.assertThat;

import org.jbehave.core.annotations.BeforeScenario;
import org.jbehave.core.annotations.Given;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;

import java.util.Stack;
import org.junit.Before;
import tech.testra.jvm.plugin.jbehave.utils.TestraJunitHelper;

public class JBehaveSteps {

  @BeforeScenario
  public void scenarioSetup(){
    //TODO thread issues
    TestraJunitHelper junitHelper = new TestraJunitHelper();
    junitHelper.checkSkip();
  }

  private Stack<String> stack = new Stack<>();

  @Given("an empty stack")
  public void createEmptyStack() {
    stack.clear();
  }

  @Given("a stack with elements")
  public void createStack() {
    stack.clear();
    stack.push("a");
    stack.push("b");
    stack.push("c");
  }

  @When("I add $number elements")
  public void addElements(int elementCount) {
    for (int i = 0; i < elementCount; i++) {
      stack.add((new Integer(i)).toString());
    }
  }

  @When("I clear stack")
  public void clearStack() {
    stack.clear();
  }

  @Then("the stack should have $number elements")
  public void assertElementCount(int elementCount) {
    assertThat(stack)
        .hasSize(elementCount);
  }
}