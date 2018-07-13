package tech.testra.jvm.plugin.junit5.test;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import tech.testra.jvm.plugin.junit5.Testra;

public class TestraJunitRunner {
    @Test
    void shouldProcessPassedTests() {
        runClasses(AssumptionTests.class);
    }
    private void runClasses(Class<?>... classes) {
        final ClassSelector[] classSelectors = Stream.of(classes)
            .map(DiscoverySelectors::selectClass)
            .toArray(ClassSelector[]::new);
        final LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
            .selectors(classSelectors)
            .build();

        final Launcher launcher = LauncherFactory.create();
        launcher.execute(request, new Testra());
    }
}
