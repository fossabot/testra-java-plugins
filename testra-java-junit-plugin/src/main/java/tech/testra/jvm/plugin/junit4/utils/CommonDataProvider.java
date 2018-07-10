package tech.testra.jvm.plugin.junit4.utils;

import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CommonDataProvider {

  private final static ConcurrentHashMap<Long, CommonData> commonDataMap = new ConcurrentHashMap<>();
  private final static Logger LOGGER = LoggerFactory.getLogger(CommonDataProvider.class);

  private CommonDataProvider() {
  }

  public static void newCommonData(Long uniqueId) {
    commonDataMap.put(uniqueId, new CommonData());
  }

  public static CommonData get(Long uniqueId) {
    return commonDataMap.get(uniqueId);
  }
}
