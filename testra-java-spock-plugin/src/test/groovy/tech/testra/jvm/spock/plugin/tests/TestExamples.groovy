package tech.testra.jvm.spock.plugin.tests

import spock.lang.Specification

import java.util.function.Function

class TestExamples extends Specification{
    void TestWithExamples() {
        given:
            true

        when:
            true

        then:
        stakeValue == expectedResult

        where:
        stakeValue | amount || expectedBalance | expectedResult
        5.0        | 10.0   || 0.0             | 5.0
        0.0        | 10.0   || 0.0             | 10.0
        10.0       | 10.0   || 0.0             | 0.0
        15.0       | 10.0   || 5.0             | 0.0
        5.0        | 15.0   || 0.0             | 10.0
        5.0        | 20.0   || 0.0             | 15.0
        10.0       | 15.0   || 0.0             | 5.0
    }

}
