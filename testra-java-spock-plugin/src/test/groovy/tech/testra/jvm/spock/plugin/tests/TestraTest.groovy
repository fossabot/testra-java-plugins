package tech.testra.jvm.spock.plugin.tests

import org.junit.Test

class TestraTest {

    @Test
    void shouldStartTest(){
        TestraRunner.run(TestGroovy)
    }

}