package com.williamhill.whgtf.testra.jvm.plugin.templates;

import gherkin.formatter.model.Tag;
import java.util.Deque;
import java.util.LinkedList;
import lombok.Data;

@Data
public class ScenarioTemplate {
  private String name;
  private String featureName;
  private Deque<StepTemplate> backgroundSteps = new LinkedList<>();
  private Deque<StepTemplate> steps = new LinkedList<>();
  private Deque<Tag> tags = new LinkedList<>();
  private ErrorTemplate error;
  private Boolean isFailed = false;
  private Boolean isSkipped = false;

}