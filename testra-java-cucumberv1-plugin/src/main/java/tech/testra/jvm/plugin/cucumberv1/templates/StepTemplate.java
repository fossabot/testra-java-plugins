package tech.testra.jvm.plugin.cucumberv1.templates;

import lombok.Data;
import tech.testra.java.client.model.TestResultRequest.ResultEnum;

@Data
public class StepTemplate {
  private int index;
  private String gherkinStep;
  private int line;
  private ResultEnum status;
  private long stepDuration;
}
