package tech.testra.jvm.plugin.cucumberv1.templates;

import lombok.Data;
import tech.testra.jvm.plugin.cucumberv1.enums.StatusEnum;

@Data
public class StepTemplate {
  private int index;
  private String gherkinStep;
  private int line;
  private StatusEnum status;
  private long stepDuration;
}
