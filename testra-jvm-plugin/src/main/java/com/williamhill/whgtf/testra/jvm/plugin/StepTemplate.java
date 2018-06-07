package com.williamhill.whgtf.testra.jvm.plugin;

import lombok.Data;

@Data
public class StepTemplate {
  private int index;
  private String gherkinStep;
  private int line;
  private StatusEnum status;
  private long stepDuration;
}
