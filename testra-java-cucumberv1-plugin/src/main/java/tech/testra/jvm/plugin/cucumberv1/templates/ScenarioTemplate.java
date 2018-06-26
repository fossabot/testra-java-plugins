package tech.testra.jvm.plugin.cucumberv1.templates;

import gherkin.formatter.model.Tag;
import lombok.Data;

import java.util.Deque;
import java.util.LinkedList;

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
