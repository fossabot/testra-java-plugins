package tech.testra.jvm.plugin.junit4.test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assume.assumeThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import tech.testra.jvm.commons.Tag;

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
}
