package tech.testra.jvm.plugin.junit.test;


import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class JunitTestClass {

  @Test
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
