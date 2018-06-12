package test.steps;

import com.williamhill.whgtf.testra.jvm.pluginv2.TestraCucumberJvmV2;
import cucumber.api.PendingException;
import cucumber.api.java.After;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import java.net.MalformedURLException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class SeleniumSteps extends AbstractSeleniumClass {

  private WebDriver webDriver;
  @FindBy(id = "shopping")
  private WebElement elmShopping;
  @FindBy(name = "userName")
  private WebElement elmUsername;

  public SeleniumSteps() {
  }


  @Given("^the webdriver is loaded$")
  public void the_webdriver_is_loaded() {
    try {
      webDriver = getNewWebDriver();
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }
  }


  @Then("^the user can click the shopping button$")
  public void theUserCanClickTheShoppingButton() {
    String actualTitle = "";
    actualTitle = webDriver.getTitle();
    elmShopping.click();

  }

  @And("^the webpage is open$")
  public void theWebpageIsOpen(){

    String baseUrl = "http://demo.guru99.com/test/newtours/";
    String expectedTitle = "Welcome: Mercury Tours";


    webDriver.get(baseUrl);
    try {
      Thread.sleep(5);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    elmUsername.click();

  }

  @After
  public void teardown(){
    byte[] screenshot = ((TakesScreenshot) webDriver).getScreenshotAs(OutputType.BYTES);
    TestraCucumberJvmV2.setScreenShot(screenshot);
    webDriver.close();
  }
}
