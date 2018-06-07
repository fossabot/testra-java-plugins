package com.williamhill.whgtf.testra.jvm.plugin;

import com.williamhill.whgtf.test.bnw.pojo.steps.StepRequest;
import java.util.List;
import lombok.Data;

@Data
public class ScenarioTemplate {
  private String name;
  private String featureName;
  private List<StepRequest> backgroundSteps;
  private List<StepRequest> steps;

}
