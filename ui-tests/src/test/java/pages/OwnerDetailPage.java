package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.stream.Collectors;

public class OwnerDetailPage extends BasePage {

    // Owner info -- first table in owner-details
    private static final By OWNER_NAME = By.cssSelector("owner-details td b");
    private static final By INFO_CELLS = By.cssSelector("owner-details table:first-of-type td.ng-binding");

    // Action buttons
    private static final By EDIT_OWNER = By.cssSelector("a[href*='/edit']");
    private static final By ADD_PET    = By.cssSelector("a[ui-sref^='petNew']");

    // Pets table
    private static final By PET_ROWS  = By.cssSelector("owner-details tr[ng-repeat]");
    private static final By PET_LINKS = By.cssSelector("owner-details a[ui-sref^='petEdit']");

    public OwnerDetailPage(WebDriver driver) {
        super(driver);
        // Wait for component tag, then wait for name to be populated by Angular
        waitForComponent("owner-details");
        wait.until(d -> {
            List<WebElement> elements = d.findElements(OWNER_NAME);
            return !elements.isEmpty() && !elements.get(0).getText().trim().isEmpty();
        });
    }

    public String getOwnerName() {
        return driver.findElement(OWNER_NAME).getText().trim();
    }

    public String getAddress() {
        List<WebElement> cells = driver.findElements(INFO_CELLS);
        return cells.get(0).getText().trim();
    }

    public String getCity() {
        List<WebElement> cells = driver.findElements(INFO_CELLS);
        return cells.get(1).getText().trim();
    }

    public String getTelephone() {
        List<WebElement> cells = driver.findElements(INFO_CELLS);
        return cells.get(2).getText().trim();
    }

    public List<String> getPetNames() {
        return driver.findElements(PET_LINKS)
            .stream()
            .map(el -> el.getText().trim())
            .collect(Collectors.toList());
    }

    public boolean hasPet(String petName) {
        return getPetNames().stream()
            .anyMatch(name -> name.equalsIgnoreCase(petName));
    }

    public int getPetCount() {
        return driver.findElements(PET_ROWS).size();
    }
}
