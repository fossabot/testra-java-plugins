package tech.testra.jvm.spock.plugin.tests

import org.junit.runner.notification.RunNotifier
import org.spockframework.runtime.JUnitDescriptionGenerator
import org.spockframework.runtime.RunContext
import org.spockframework.runtime.SpecInfoBuilder
import org.spockframework.runtime.model.SpecInfo
import tech.testra.jvm.spock.plugin.Testra

class TestraRunner {

    private final static NOTIFIER = new RunNotifier()
    static void run(Class clazz){
        SpecInfo spec = new SpecInfoBuilder(clazz).build()
        spec.addListener(new Testra())
        new JUnitDescriptionGenerator(spec).describeSpecMethods()
        new JUnitDescriptionGenerator(spec).describeSpec()
        RunContext.get().createSpecRunner(spec, NOTIFIER).run()
    }
}