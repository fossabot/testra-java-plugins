package com.williamhill.whgtf.testra.jvm.pluginv2;

import com.williamhill.whgtf.testra.jvm.pluginv2.enums.StatusEnum;
import lombok.Data;

@Data
public class StepTemplate {
  private int index;
  private String gherkinStep;
  private int line;
  private StatusEnum status;
  private long stepDuration;
}
