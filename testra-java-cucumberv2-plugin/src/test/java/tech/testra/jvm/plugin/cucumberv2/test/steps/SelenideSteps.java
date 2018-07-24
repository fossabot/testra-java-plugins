package tech.testra.jvm.plugin.cucumberv2.test.steps;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.open;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Configuration;
import cucumber.api.Scenario;
import cucumber.api.java.After;
import cucumber.api.java.en.Given;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriverException;

public class SelenideSteps {

  public SelenideSteps() {
  }

  @Given("^Nothing in the background$")
  public void nothing() {
    Configuration.browser = "chrome";
    Configuration.startMaximized = true;
    Configuration.holdBrowserOpen = true;
    setChromePath();
  }

  @Given("^the selenide user searches google$")
    public void user_can_search_everything_in_google() {
      open("https://www.google.com");
      $(By.name("q")).val("selenide").pressEnter();

      $$("#ires .g").shouldHave(size(10));

      $("#ires .g").shouldBe(Condition.visible).shouldHave(
          text("Selenide: concise UI tests in Java"),
          text("selenide.org"));
    }

  @Given("^the selenide user searches google fail$")
  public void user_can_search_everything_in_google_fail() {
    open("https://www.google.com");
    $(By.name("q")).val("selenide").pressEnter();

    $$("#ires .g").shouldHave(size(10));

    $("#ires .g").shouldBe(Condition.visible).shouldHave(
        text("cheeseburger"),
        text("selenide.org"));
  }

  @After
  public void teardown(Scenario scenario){
    if(getWebDriver()!=null){
      try {
        if (scenario.isFailed()) {
          byte[] screenshot = ((TakesScreenshot) getWebDriver()).getScreenshotAs(OutputType.BYTES);
          scenario.embed(screenshot, "image/png");
        }
      } catch (WebDriverException | ClassCastException somePlatformsDontSupportScreenshots) {
        somePlatformsDontSupportScreenshots.printStackTrace();
      }
    }
  }

  protected static void setChromePath() {
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
  }
}