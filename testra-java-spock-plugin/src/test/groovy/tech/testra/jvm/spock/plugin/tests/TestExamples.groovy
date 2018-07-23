package tech.testra.jvm.spock.plugin.tests

import spock.lang.Specification
import spock.lang.Unroll

import java.util.function.Function

@Unroll
class TestExamples extends Specification{
    def 'Should add #initValue and #amount to the pot with max limit equals #maxValue'() {
        given:
            true
        when:
            true
        then:
            maxValue == expectedRestAmount

        where:
        initValue | amount | maxValue || expectedRestAmount | expectedValue
        1.0       | 10.0   | 5.0      || 6.0                | 5.0
        11.0      | 1.0    | 5.0      || 7.0                | 5.0
        0.0       | 10.0   | 5.0      || 5.0                | 5.0
    }

}
