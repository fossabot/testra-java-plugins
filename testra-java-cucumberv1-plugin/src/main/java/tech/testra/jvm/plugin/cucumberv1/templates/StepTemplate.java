package tech.testra.jvm.plugin.cucumberv1.templates;

import gherkin.formatter.model.Result;
import lombok.Data;

@Data
public class StepTemplate {
  private int index;
  private String gherkinStep;
  private int line;
  private Result status;
  private long stepDuration;
}
