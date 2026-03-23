package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public abstract class BasePage {

    protected final WebDriver driver;
    protected final WebDriverWait wait;

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    protected BasePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, TIMEOUT);
    }

    protected WebElement waitForVisible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    protected WebElement waitForClickable(By locator) {
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    protected void waitForTextPresent(By locator, String text) {
        wait.until(ExpectedConditions.textToBePresentInElementLocated(locator, text));
    }

    protected void waitForComponent(String tagName) {
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName(tagName)));
    }

    /**
     * Scrolls an element into the center of the viewport and clicks it
     * via JavaScript, bypassing the fixed chatbox widget that intercepts
     * native Selenium clicks on elements near the bottom of the page.
     * Use this instead of waitForClickable().click() for all clickable elements.
     */
    protected void jsClick(By locator) {
        WebElement element = waitForVisible(locator);
        ((JavascriptExecutor) driver)
            .executeScript("arguments[0].scrollIntoView({block: 'center'}); arguments[0].click();", element);
    }

    /**
     * Scrolls a known element into the center of the viewport and clicks
     * it via JavaScript. Use when you already have the WebElement reference.
     */
    protected void jsClick(WebElement element) {
        ((JavascriptExecutor) driver)
            .executeScript("arguments[0].scrollIntoView({block: 'center'}); arguments[0].click();", element);
    }

    /**
     * Temporarily hides the chatbox, performs a native Selenium click,
     * then restores the chatbox. Use for form submit buttons where
     * Angular's ng-submit requires a real browser click event to fire
     * but the chatbox would otherwise intercept it.
     */
    protected void scrollAndClick(By locator) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript(
            "var chatbox = document.getElementById('chatbox');" +
                "if (chatbox) chatbox.style.display = 'none';"
        );
        try {
            waitForClickable(locator).click();
        } finally {
            js.executeScript(
                "var chatbox = document.getElementById('chatbox');" +
                    "if (chatbox) chatbox.style.display = '';"
            );
        }
    }

    /**
     * Moves to an element and clicks it using the Actions API.
     * This synthesizes a real mouse event at the element center,
     * which Angular's ng-submit responds to correctly and which
     * is not blocked by overlapping fixed elements.
     */
    protected void actionsClick(By locator) {
        WebElement element = waitForClickable(locator);
        ((JavascriptExecutor) driver)
            .executeScript("arguments[0].scrollIntoView({block: 'center'});", element);
        new org.openqa.selenium.interactions.Actions(driver)
            .moveToElement(element)
            .click()
            .perform();
    }
}
