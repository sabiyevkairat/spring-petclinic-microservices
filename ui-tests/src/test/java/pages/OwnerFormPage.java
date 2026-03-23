package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public class OwnerFormPage extends BasePage {

    public static final By FIRST_NAME = By.cssSelector("input[ng-model='$ctrl.owner.firstName']");
    public static final By LAST_NAME = By.cssSelector("input[ng-model='$ctrl.owner.lastName']");
    public static final By ADDRESS = By.cssSelector("input[ng-model='$ctrl.owner.address']");
    public static final By CITY = By.cssSelector("input[ng-model='$ctrl.owner.city']");
    public static final By TELEPHONE = By.cssSelector("input[ng-model='$ctrl.owner.telephone']");

    private static final By SUBMIT = By.cssSelector("button[type='submit']");

    public OwnerFormPage(WebDriver driver) {
        super(driver);
        waitForComponent("owner-form");
    }

    public OwnerFormPage waitForPrePopulation() {
        wait.until(d -> {
            List<WebElement> elements = d.findElements(FIRST_NAME);
            if (elements.isEmpty()) return false;
            String val = elements.get(0).getAttribute("value");
            return val != null && !val.trim().isEmpty();
        });
        return this;
    }

    public OwnersPage submit(String firstName, String lastName,
                                  String address, String city, String telephone) {
        fill(firstName, lastName, address, city, telephone);
        actionsClick(SUBMIT);
        return new OwnersPage(driver);
    }


    public OwnerDetailPage submitForm() {
        actionsClick(SUBMIT);
        return new OwnerDetailPage(driver);
    }

    public OwnerFormPage fill(String firstName, String lastName,
                              String address, String city, String telephone) {
        fillField(FIRST_NAME, firstName);
        fillField(LAST_NAME, lastName);
        fillField(ADDRESS, address);
        fillField(CITY, city);
        fillField(TELEPHONE, telephone);
        return this;
    }

    public OwnerFormPage fillField(By locator, String value) {
        WebElement field = waitForVisible(locator);
        field.clear();
        field.sendKeys(value);
        return this;
    }

    public String getFieldValue(By locator) {
        return waitForVisible(locator).getAttribute("value");
    }

}
