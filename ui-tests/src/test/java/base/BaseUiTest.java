package base;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pages.OwnerDetailPage;
import pages.OwnerFormPage;
import pages.OwnersPage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

/**
 * Base class for all UI tests.
 *
 * Manages WebDriver lifecycle — creates a fresh browser before each test
 * and tears it down after, taking a screenshot on failure.
 *
 * Configuration via system properties (set in pom.xml from env vars):
 *   base.url  — UI base URL (default: http://localhost:8080)
 *   headless  — run Chrome headless (default: true)
 */
public abstract class BaseUiTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseUiTest.class);

    private static final String DEFAULT_BASE_URL = "http://localhost:8080";
    private static final Duration IMPLICIT_WAIT   = Duration.ofSeconds(5);
    private static final Duration PAGE_LOAD_TIMEOUT = Duration.ofSeconds(30);

    protected WebDriver driver;
    protected String baseUrl;

    @BeforeEach
    void setUpDriver(TestInfo testInfo) {
        baseUrl = resolveBaseUrl();
        boolean headless = resolveHeadless();

        LOGGER.info("Starting test: {}", testInfo.getDisplayName());
        LOGGER.info("Base URL: {} | Headless: {}", baseUrl, headless);

        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        if (headless) {
            options.addArguments("--headless=new");
        }
        options.addArguments(
            "--no-sandbox",
            "--disable-dev-shm-usage",
            "--disable-gpu",
            "--window-size=1920,1080"
        );

        driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(IMPLICIT_WAIT);
        driver.manage().timeouts().pageLoadTimeout(PAGE_LOAD_TIMEOUT);
        driver.manage().window().maximize();
    }

    @AfterEach
    void tearDownDriver(TestInfo testInfo) {
        if (driver != null) {
            takeScreenshotOnFailure(testInfo);
            driver.quit();
        }
    }

    /**
     * Navigates to a path relative to the base URL.
     * Example: navigateTo("/owners") → http://localhost:8080/owners
     */
    protected void navigateTo(String path) {
        driver.get(baseUrl + path);
    }

    protected OwnersPage openOwnersPage() {
        navigateTo("/#!/owners");
        return new OwnersPage(driver);
    }

    /**
     * Navigates directly to an owner detail page by ID.
     */
    protected OwnerDetailPage openOwnerDetail(int ownerId) {
        navigateTo("/#!/owners/details/" + ownerId);
        return new OwnerDetailPage(driver);
    }

    /**
     * Navigates to the add new owner form
     */
    protected OwnerFormPage openNewOwnerForm() {
        navigateTo("#!/owners/new");
        return new OwnerFormPage(driver);
    }

    /**
     * Navigates to the edit form for an existing owner.
     */
    protected OwnerFormPage openEditOwnerForm(int ownerId) {
        navigateTo("/#!/owners/" + ownerId + "/edit");
        return new OwnerFormPage(driver);
    }

    private static String resolveBaseUrl() {
        String url = System.getProperty("base.url");
        if (url == null || url.isBlank() || url.equals("null")) {
            return DEFAULT_BASE_URL;
        }
        return url;
    }

    private static boolean resolveHeadless() {
        String headless = System.getProperty("headless");
        if (headless == null || headless.isBlank() || headless.equals("null")) {
            return true; // default to headless
        }
        return Boolean.parseBoolean(headless);
    }

    private void takeScreenshotOnFailure(TestInfo testInfo) {
        // JUnit 5 does not expose test failure status in @AfterEach directly.
        // Screenshots are taken unconditionally and saved with the test name.
        // In CI, the upload-artifact step captures the screenshots/ directory.
        try {
            byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            String safeName = testInfo.getDisplayName()
                .replaceAll("[^a-zA-Z0-9_\\-]", "_")
                .replaceAll("_+", "_");
            Path dir = Paths.get("target", "screenshots");
            Files.createDirectories(dir);
            Path file = dir.resolve(safeName + ".png");
            Files.write(file, screenshot);
            LOGGER.info("Screenshot saved: {}", file);
        } catch (IOException e) {
            LOGGER.warn("Could not save screenshot: {}", e.getMessage());
        }
    }
}
