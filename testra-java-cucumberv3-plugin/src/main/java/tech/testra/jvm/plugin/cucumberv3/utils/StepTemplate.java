package tech.testra.jvm.plugin.cucumberv3.utils;

import cucumber.api.Result;
import lombok.Data;

@Data
public class StepTemplate {
  private int index;
  private String gherkinStep;
  private int line;
  private Result.Type status;
  private long stepDuration;
}
