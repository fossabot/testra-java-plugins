package tech.testra.jvm.plugin.junit4.test;


import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;
import tech.testra.jvm.plugin.junit4.Testra;

public class TestraJunitRunner extends BlockJUnit4ClassRunner {

    public TestraJunitRunner(Class<?> klass) throws InitializationError {
        super(klass);
    }

    @Override public void run(RunNotifier notifier){
        notifier.addListener(new Testra());
        notifier.fireTestRunStarted(getDescription());
        super.run(notifier);
    }
}