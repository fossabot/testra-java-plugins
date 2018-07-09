package tech.testra.jvm.plugin.cucumberv3.test.steps;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class AbstractSeleniumClass {

    protected static WebDriver getNewWebDriver() {
      Optional<Path> chromeDriverPath = Optional.empty();
      try {
        String driverPath = "testra-jvm-cucumberv2-plugin/src/test/resources/webdriver";
        if (!Files.exists(Paths.get(driverPath))) {
          driverPath = driverPath.substring(driverPath.indexOf("/")).substring(1);
        }
        chromeDriverPath = Files.walk(Paths.get(driverPath))
            .filter(Files::isRegularFile)
            .filter(path -> path.endsWith("chromedriver"))
            .findFirst();
      } catch (IOException e) {
        e.printStackTrace();
      }
      if (chromeDriverPath.isPresent()) {
        System.setProperty("webdriver.chrome.driver",
            chromeDriverPath.get().toAbsolutePath().toString());
      } else {
        throw new IllegalStateException("Chrome driver binary not found");
      }
      WebDriver driver = new ChromeDriver();

    return driver;
  }

}
