package tech.testra.jvm.plugin.junit5.test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assume.assumeThat;

import org.junit.Test;
import org.junit.jupiter.api.Tag;
import org.junit.runner.RunWith;


@Tag("Assumptions")
//@RunWith(TestraJunitRunner.class)
public class AssumptionTests {

  @Test
  @Tag("FailedTest")
  public void assumptionFailedTest() {
    assumeThat(true, is(false));
  }

  @Test
  @Tag("PassingTest")
  public void assumptionTrueTest() {
    assumeThat(true, is(true));
  }

  @Test
  @Tag("PassingTest2")
  public void assumptionTrueTest2() {
    assumeThat(true, is(true));
  }
}
