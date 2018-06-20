package tech.testra.jvm.plugin.cucumberv1.templates;

import lombok.Data;
import tech.testra.jvm.client.model.Result;
import tech.testra.jvm.client.model.TestResultRequest.ResultEnum;
import tech.testra.jvm.plugin.cucumberv1.enums.StatusEnum;

@Data
public class StepTemplate {
  private int index;
  private String gherkinStep;
  private int line;
  private ResultEnum status;
  private long stepDuration;
}
