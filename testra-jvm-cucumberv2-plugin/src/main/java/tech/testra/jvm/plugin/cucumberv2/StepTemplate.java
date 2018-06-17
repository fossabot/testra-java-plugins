package tech.testra.jvm.plugin.cucumberv2;

import lombok.Data;
import tech.testra.jvm.plugin.cucumberv2.enums.StatusEnum;

@Data
public class StepTemplate {
  private int index;
  private String gherkinStep;
  private int line;
  private StatusEnum status;
  private long stepDuration;
}
