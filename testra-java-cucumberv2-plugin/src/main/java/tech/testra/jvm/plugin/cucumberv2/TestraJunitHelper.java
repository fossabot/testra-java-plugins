package tech.testra.jvm.plugin.cucumberv2;

import static tech.testra.jvm.commons.util.PropertyHelper.prop;
import org.junit.AssumptionViolatedException;
import tech.testra.jvm.plugin.cucumberv2.utils.CommonData;
import tech.testra.jvm.plugin.cucumberv2.utils.CommonDataProvider;

public class TestraJunitHelper {

  private final boolean isRerun;
  private final Long threadId = Thread.currentThread().getId();
  private CommonData commonData;

  public TestraJunitHelper(){
    isRerun = Boolean.parseBoolean(prop("isrerun"));
  }

  public void checkSkip() {
    commonData = CommonDataProvider.get(threadId);
    if(isRerun) {
      if (!commonData.failedScenarioIDs.containsKey(commonData.currentScenarioID)) {
        commonData.skip = true;
        throw new AssumptionViolatedException("Test already passed, skipping");
      }
    }
  }

}
