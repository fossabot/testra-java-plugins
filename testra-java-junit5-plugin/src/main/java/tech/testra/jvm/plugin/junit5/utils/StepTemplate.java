package tech.testra.jvm.plugin.junit5.utils;

import lombok.Data;
import tech.testra.java.client.model.Result;

@Data
public class StepTemplate {
  private int index;
  private String gherkinStep;
  private int line;
  private Result status;
  private long stepDuration;
}
