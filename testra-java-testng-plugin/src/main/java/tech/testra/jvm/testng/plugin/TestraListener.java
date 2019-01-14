package tech.testra.jvm.testng.plugin;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;
import tech.testra.java.client.model.TestResultRequest;
import tech.testra.java.client.model.Testcase;
import tech.testra.java.client.model.TestcaseRequest;
import tech.testra.jvm.reporter.TestraReporter;
import tech.testra.jvm.reporter.TestraTestcaseReporter;
import tech.testra.jvm.reporter.exception.TestAlreadyPassed;

import java.util.Arrays;

import static java.util.stream.Collectors.joining;

public class TestraListener implements ITestListener {

  private static TestraTestcaseReporter testraTestCaseReporter = TestraTestcaseReporter.instance();

  @Override
  public void onStart(ITestContext arg0) {}

  @Override
  public void onTestStart(ITestResult result) {
    if (TestraReporter.isTestraEnabled()) {
      TestcaseRequest testCaseRequest = new TestcaseRequest();
      String fullyQualifiedClassName = result.getTestClass().getName();
      String testCaseName = result.getMethod().getMethodName();
      if (result.getParameters().length > 0) {
        String delimittedParams =
            Arrays.stream(result.getParameters()).map(Object::toString).collect(joining(","));

        testCaseName = testCaseName + " [" + delimittedParams + "]";
      }
      testCaseRequest
          .name(testCaseName)
          .className(
              fullyQualifiedClassName.substring(fullyQualifiedClassName.lastIndexOf(".") + 1))
          .namespace(
              fullyQualifiedClassName.substring(0, fullyQualifiedClassName.lastIndexOf(".")));
      testraTestCaseReporter.setCurrentTestCase(
          testraTestCaseReporter.createTestcase(testCaseRequest));
    }
  }

  @Override
  public void onTestSuccess(ITestResult result) {
    if (TestraReporter.isTestraEnabled()) {
      TestResultRequest testResultRequest =
          new TestResultRequest()
              .status(TestResultRequest.StatusEnum.PASSED)
              .startTime(result.getStartMillis())
              .endTime(result.getEndMillis())
              .durationInMs(result.getEndMillis() - result.getStartMillis());
      this.onTestFinish(testResultRequest);
    }
  }

  @Override
  public void onTestSkipped(ITestResult result) {
    Testcase currentTestCase = testraTestCaseReporter.getCurrentTestCase();
    if (currentTestCase != null) {
      TestResultRequest testResultRequest =
          new TestResultRequest()
              .startTime(result.getStartMillis())
              .endTime(result.getEndMillis())
              .status(TestResultRequest.StatusEnum.SKIPPED);
      this.onTestFinish(testResultRequest);
    }
  }

  @Override
  public void onTestFailure(ITestResult result) {
    if (TestraReporter.isTestraEnabled() && !(result.getThrowable() instanceof TestAlreadyPassed)) {
      TestResultRequest testResultRequest =
          new TestResultRequest()
              .startTime(result.getStartMillis())
              .endTime(result.getEndMillis())
              .durationInMs(result.getEndMillis() - result.getStartMillis())
              .status(TestResultRequest.StatusEnum.FAILED)
              .error(ExceptionUtils.getStackTrace(result.getThrowable()));
      this.onTestFinish(testResultRequest);
    }
  }

  private void onTestFinish(TestResultRequest testResultRequest) {
    testraTestCaseReporter.createResult(testResultRequest);
  }

  @Override
  public void onTestFailedButWithinSuccessPercentage(ITestResult arg0) {}

  @Override
  public void onFinish(ITestContext arg0) {}
}
