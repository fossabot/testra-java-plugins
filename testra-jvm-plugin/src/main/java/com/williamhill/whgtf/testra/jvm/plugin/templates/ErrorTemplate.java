package com.williamhill.whgtf.testra.jvm.plugin.templates;

import java.util.List;
import lombok.Data;
import org.junit.ComparisonFailure;

@Data
public class ErrorTemplate {
  private int lineNumber;
  private String gherkinStep;
  private Byte[] attachment;
  private String errorMessage;
  private String localizedErrorMessage;
  private StackTraceElement[] stackTrace;

}
