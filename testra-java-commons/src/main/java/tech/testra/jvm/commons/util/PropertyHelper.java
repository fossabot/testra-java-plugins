package tech.testra.jvm.commons.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PropertyHelper {

  private static final Logger LOGGER = LoggerFactory.getLogger(PropertyHelper.class);
  private static Properties properties = new Properties();
  private static List<String> resourceFileNames = new ArrayList<>();

  private PropertyHelper() {
  }

  @SuppressWarnings("squid:S00112")
  public static synchronized void loadProperties(String resourceFileName, ClassLoader classLoader) {
    try {
      if (!resourceFileNames.contains(resourceFileName)) {
        LOGGER.info("Loading property resource : {}", resourceFileName);
        properties.load(classLoader.getResourceAsStream(resourceFileName));
        resourceFileNames.add(resourceFileName);
      }
    } catch (IOException ex) {
      ex.fillInStackTrace();
      throw new RuntimeException(
          String.format("Property resource loading error for %s", resourceFileName));
    }
  }

  public static synchronized void loadPropertiesFromAbsolute(String resourceFileName) {
    try {
      if (!resourceFileNames.contains(resourceFileName)) {
        LOGGER.info("Loading property resource : {}", resourceFileName);
        FileInputStream in = new FileInputStream(resourceFileName);
        properties.load(in);
        resourceFileNames.add(resourceFileName);
      }
    } catch (IOException ex) {
      ex.fillInStackTrace();
      throw new RuntimeException(
          String.format("Property resource loading error for %s", resourceFileName));
    }
  }


  public static String prop(String value) {
    return properties.getProperty(value);
  }

  public static String prop(String value, String defaultValue) {
    return properties.getProperty(value, defaultValue);
  }

  public static String sysArg(String arg) {
    return System.getProperty(arg);
  }

  public static String sysArg(String arg, String defaultValue) {
    return System.getProperty(arg, defaultValue);
  }

  public static String getEnv() {
    return sysArg("env", "local");
  }

  public static boolean isBrowserstack() {
    return sysArg("browserstack", "false").equals("true");
  }

  public static boolean isRemoteDriver() {
    return sysArg("remote", "false").equals("true");
  }
}
