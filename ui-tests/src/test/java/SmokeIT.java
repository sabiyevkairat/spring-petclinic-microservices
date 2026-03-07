import base.BaseUiTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UI-01 — Smoke test confirming the PetClinic frontend is reachable
 * and the home page loads correctly.
 */
@DisplayName("UI Smoke Tests")
class SmokeIT extends BaseUiTest {

    @Test
    @DisplayName("Home page loads and displays PetClinic title")
    void homePageLoads() {
        navigateTo("/");

        assertThat(driver.getTitle())
            .as("Page title should contain PetClinic")
            .containsIgnoringCase("PetClinic");
    }

    @Test
    @DisplayName("Welcome page contains navigation links")
    void homePageHasNavigation() {
        navigateTo("/");

        String pageSource = driver.getPageSource();
        assertThat(pageSource)
            .as("Navigation should contain Owners link")
            .containsIgnoringCase("owners");
        assertThat(pageSource)
            .as("Navigation should contain Vets link")
            .containsIgnoringCase("veterinarians");
    }
}
