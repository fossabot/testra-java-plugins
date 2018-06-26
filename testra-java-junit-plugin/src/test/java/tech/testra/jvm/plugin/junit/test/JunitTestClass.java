package tech.testra.jvm.plugin.junit.test;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import tech.testra.jvm.commons.Epic;
import tech.testra.jvm.commons.Feature;

import static org.assertj.core.api.Assertions.assertThat;

public class JunitTestClass {

  @TestFactory
  @Test
  @Epic("Test Epic")
  @Feature("feature1")
  public void multiplicationTest(){
      assertThat(2*2)
          .as("multiplication of 2 and 2")
          .isEqualTo(4);
  }

  private String developer = "Paul Johnson";

  @Test
  public void assertTrueTest() {
    assertThat(true).isEqualTo(true);
  }

  @Test
  public void equalToTest() {
    assertThat(developer).isEqualTo("Paul");
  }


}
