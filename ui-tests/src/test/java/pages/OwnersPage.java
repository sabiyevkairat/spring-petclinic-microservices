package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.stream.Collectors;

public class OwnersPage extends BasePage {

    private static final By SEARCH_INPUT = By.cssSelector("input[ng-model='$ctrl.query']");
    private static final By OWNER_ROWS   = By.cssSelector("owner-list tbody tr[ng-repeat]");
    private static final By OWNER_LINKS  = By.cssSelector("owner-list tbody a[ui-sref^='ownerDetails']");

    public OwnersPage(WebDriver driver) {
        super(driver);
        waitForComponent("owner-list");
    }

    public OwnersPage search(String query) {
        WebElement input = waitForVisible(SEARCH_INPUT);
        input.clear();
        input.sendKeys(query);
        return this;
    }

    public List<String> getOwnerNames() {
        return driver.findElements(OWNER_LINKS)
            .stream()
            .map(el -> el.getText().trim())
            .collect(Collectors.toList());
    }

    public int getRowCount() {
        return driver.findElements(OWNER_ROWS).size();
    }

    public OwnerDetailPage clickOwner(String fullName) {
        waitForVisible(OWNER_LINKS);
        WebElement link = driver.findElements(OWNER_LINKS)
            .stream()
            .filter(el -> el.getText().trim().equals(fullName))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Owner not found in list: " + fullName));
        ((JavascriptExecutor) driver)
            .executeScript("arguments[0].scrollIntoView({block: 'center'}); arguments[0].click();", link);
        return new OwnerDetailPage(driver);
    }

    public List<String> searchAndGetNames(String query) {
        return search(query).getOwnerNames();
    }
}
