package tech.testra.jvm.plugin.cucumberv1.templates;

import lombok.Data;

@Data
public class ErrorTemplate {
  private int lineNumber;
  private String gherkinStep;
  private Byte[] attachment;
  private String errorMessage;
  private String localizedErrorMessage;
  private StackTraceElement[] stackTrace;
}
