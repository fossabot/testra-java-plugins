package tech.testra.jvm.plugin.jbehave.test;

import static java.util.Collections.singletonList;

import java.io.File;
import org.jbehave.core.annotations.BeforeScenario;
import org.jbehave.core.configuration.MostUsefulConfiguration;
import org.jbehave.core.embedder.Embedder;
import org.jbehave.core.embedder.EmbedderControls;
import org.jbehave.core.io.LoadFromClasspath;
import org.jbehave.core.reporters.StoryReporterBuilder;
import org.jbehave.core.steps.InstanceStepsFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import tech.testra.jvm.plugin.jbehave.Testra;
import tech.testra.jvm.plugin.jbehave.test.steps.JBehaveSteps;
import tech.testra.jvm.plugin.jbehave.utils.TestraJunitHelper;

public class JBehaveRunner {

  private Embedder embedder = new Embedder();

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  @Before
  public void setUp() throws Exception {
    embedder.useEmbedderControls(new EmbedderControls()
        .doGenerateViewAfterStories(false)
        .doVerboseFailures(true)
    );
    embedder.useConfiguration(new MostUsefulConfiguration()
        .useStoryLoader(new LoadFromClasspath(this.getClass()))
        .useStoryReporterBuilder(new ReportlessStoryReporterBuilder(folder.newFolder())
            .withReporters(new Testra())
        )
    );
    embedder.useCandidateSteps(new InstanceStepsFactory(embedder.configuration(), new JBehaveSteps())
        .createCandidateSteps()
    );
  }


  @Test
  public void runTests() {
    embedder.runStoriesAsPaths(singletonList("stories/example.story"));
  }


  static class ReportlessStoryReporterBuilder extends StoryReporterBuilder {

    private final File outputDirectory;

    ReportlessStoryReporterBuilder(final File outputDirectory) {
      this.outputDirectory = outputDirectory;
    }

    @Override
    public File outputDirectory() {
      return outputDirectory;
    }
  }



}
