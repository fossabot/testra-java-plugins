package tech.testra.jvm.reporter;

import tech.testra.java.client.TestraRestClient;
import tech.testra.java.client.model.TestResultRequest;
import tech.testra.java.client.model.Testcase;
import tech.testra.java.client.model.TestcaseRequest;
import tech.testra.jvm.reporter.exception.TestAlreadyPassed;

public final class TestraTestcaseReporter extends TestraReporter {

    private ThreadLocal<Testcase> currentTestCase = new ThreadLocal<>();

    public static TestraTestcaseReporter instance() {
        if (INSTANCE == null) {
            INSTANCE = new TestraTestcaseReporter();
        }
        return INSTANCE;
    }

    public Testcase createTestcase(TestcaseRequest testcaseRequest) {
        Testcase testcase = TestraRestClient.createTestcase(testcaseRequest);
        if (isRetryMode()) {
            if (failedTests.keySet().stream().anyMatch(key -> testcase.getId().equals(key))) {
                throw new TestAlreadyPassed();
            }
        }
        return testcase;
    }

    public void createResult(TestResultRequest testResultRequest) {
        testResultRequest
                .targetId(getCurrentTestCase().getId())
                .groupId(getCurrentTestCase().getNamespaceId())
                .resultType(TestResultRequest.ResultTypeEnum.TEST_CASE);

        if (isRetryMode()) {
            testResultRequest.retryCount(failedTests.get(getCurrentTestCase().getId()).getRetryCount());
        } else {
            testResultRequest.retryCount(0);
        }
        TestraRestClient.createResult(testResultRequest);
    }

    public Testcase getCurrentTestCase() {
        return currentTestCase.get();
    }

    public void setCurrentTestCase(Testcase testCase) {
        this.currentTestCase.set(testCase);
    }
}
