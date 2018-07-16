package tech.testra.jvm.spock.plugin.tests

import spock.lang.Specification
import tech.testra.jvm.commons.Tag


class TestGroovy extends Specification {

    void step1() {
        expect:
        true
    }

    @Tag("Manual")
    void step2() {
        expect:
        true
    }

    @Tag("ExpectedFailure")
    void step3() {
        expect:
        throw new RuntimeException("Failure")
    }
}