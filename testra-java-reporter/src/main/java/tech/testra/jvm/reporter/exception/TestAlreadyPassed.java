package tech.testra.jvm.reporter.exception;

public class TestAlreadyPassed extends RuntimeException {

    public TestAlreadyPassed() {
        super("Test already passed in previous execution");
    }
}
