package tests;

import base.BaseUiTest;
import com.github.javafaker.Faker;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pages.OwnerDetailPage;
import pages.OwnersPage;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UI-02: Owners List, Search and Detail")
class OwnersIT extends BaseUiTest {

    private static final String OWNERS_API = "/api/customer/owners";
    private static final Faker  FAKER      = new Faker();

    private int    ownerId;
    private String firstName;
    private String lastName;
    private String address;
    private String city;
    private String telephone;

    @BeforeEach
    void createOwnerViaApi() {
        RestAssured.baseURI = baseUrl;

        firstName = FAKER.name().firstName();
        lastName  = FAKER.name().lastName() + FAKER.number().digits(6);
        address   = FAKER.address().streetAddress();
        city      = FAKER.address().city();
        telephone = FAKER.number().digits(10);

        ownerId = given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(Map.of(
                "firstName", firstName,
                "lastName",  lastName,
                "address",   address,
                "city",      city,
                "telephone", telephone
            ))
            .when()
            .post(OWNERS_API)
            .then()
            .statusCode(201)
            .extract().path("id");
    }

    @Test
    @DisplayName("Owners list displays created owner")
    void ownersListDisplaysCreatedOwner() {
        OwnersPage page = openOwnersPage();

        List<String> names = page.getOwnerNames();

        assertThat(names)
            .as("Owners list should contain the created owner")
            .contains(firstName + " " + lastName);
    }

    @Test
    @DisplayName("Search by last name filters owners correctly")
    void searchByLastNameFiltersOwners() {
        OwnersPage page = openOwnersPage();

        List<String> filtered = page.searchAndGetNames(lastName);

        // The 6-digit suffix guarantees the last name is unique to this run,
        // so we can assert the filtered list contains exactly our owner
        assertThat(filtered)
            .as("Search results should contain our owner")
            .containsExactly(firstName + " " + lastName);
    }

    @Test
    @DisplayName("Search with no match shows empty table")
    void searchWithNoMatchShowsEmptyTable() {
        OwnersPage page = openOwnersPage();

        page.search("ZZZ" + FAKER.number().digits(10) + "XXX");

        assertThat(page.getRowCount())
            .as("No rows should be visible for a non-matching search")
            .isZero();
    }

    @Test
    @DisplayName("Clicking owner navigates to correct detail page")
    void clickingOwnerNavigatesToDetailPage() {
        OwnersPage page = openOwnersPage();

        OwnerDetailPage detail = page.clickOwner(firstName + " " + lastName);

        assertThat(detail.getOwnerName())
            .as("Detail page should show the correct owner name")
            .isEqualTo(firstName + " " + lastName);
    }

    @Test
    @DisplayName("Owner detail page displays all correct fields")
    void ownerDetailDisplaysCorrectFields() {
        OwnerDetailPage detail = openOwnerDetail(ownerId);

        assertThat(detail.getOwnerName()).isEqualTo(firstName + " " + lastName);
        assertThat(detail.getAddress()).isEqualTo(address);
        assertThat(detail.getCity()).isEqualTo(city);
        assertThat(detail.getTelephone()).isEqualTo(telephone);
    }

    @Test
    @DisplayName("Owner detail page shows empty pets list for new owner")
    void ownerDetailShowsNoPetsForNewOwner() {
        OwnerDetailPage detail = openOwnerDetail(ownerId);

        assertThat(detail.getPetCount())
            .as("Newly created owner should have no pets")
            .isZero();
    }
}
