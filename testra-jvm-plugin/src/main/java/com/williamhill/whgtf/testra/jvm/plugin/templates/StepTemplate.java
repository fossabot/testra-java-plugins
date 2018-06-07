package com.williamhill.whgtf.testra.jvm.plugin.templates;

import com.williamhill.whgtf.testra.jvm.plugin.enums.StatusEnum;
import lombok.Data;

@Data
public class StepTemplate {
  private int index;
  private String gherkinStep;
  private int line;
  private StatusEnum status;
  private long stepDuration;
}
