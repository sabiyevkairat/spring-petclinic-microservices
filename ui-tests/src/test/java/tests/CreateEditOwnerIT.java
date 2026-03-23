package tests;

import base.BaseUiTest;
import com.github.javafaker.Faker;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pages.OwnerDetailPage;
import pages.OwnerFormPage;
import pages.OwnersPage;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Create and edit owner tests")
public class CreateEditOwnerIT extends BaseUiTest {

    private static final String OWNER_API = "/api/customer/owners";
    private static final Faker FAKER = new Faker();
    String firstName, lastName, address, city, telephone;

    @BeforeEach
    void configureRestAssured(){
        RestAssured.baseURI = baseUrl;
        firstName = FAKER.name().firstName();
        lastName = FAKER.name().lastName();
        address = FAKER.address().streetAddress();
        city = FAKER.address().city();
        telephone = FAKER.number().digits(12);
    }

    @Test
    @DisplayName("Add owner form submits successfully and redirects to owners page")
    void addOwnerNavigatesToDetailPage() {

        OwnersPage list = openNewOwnerForm()
            .submit(firstName, lastName, address, city, telephone);

        assertThat(list.getOwnerNames())
            .as("Newly created owner should appear in the owners list")
            .contains(firstName + " " + lastName);
    }

    @Test
    @DisplayName("Add new owner persists all fields correctly")
    void addOwnerPersistsAllFields() {
        OwnersPage list = openNewOwnerForm()
            .submit(firstName, lastName, address, city, telephone);

        OwnerDetailPage ownerDetailPage = list.clickOwner(firstName + " " + lastName);
        assertThat(ownerDetailPage.getOwnerName()).isEqualTo(firstName + " " + lastName);
        assertThat(ownerDetailPage.getAddress()).isEqualTo(address);
        assertThat(ownerDetailPage.getCity()).isEqualTo(city);
        assertThat(ownerDetailPage.getTelephone()).isEqualTo(telephone);
    }

    @Test
    @DisplayName("Edit owner form is prepopulated with values")
    void editOwnerFormIsPrepopulated() {
        int ownerId = createOwner(firstName, lastName, address, city, telephone);

        OwnerFormPage ownerFormPage = openEditOwnerForm(ownerId).waitForPrePopulation();

        assertThat(ownerFormPage.getFieldValue(OwnerFormPage.FIRST_NAME)).isEqualTo(firstName);
        assertThat(ownerFormPage.getFieldValue(OwnerFormPage.LAST_NAME)).isEqualTo(lastName);
        assertThat(ownerFormPage.getFieldValue(OwnerFormPage.ADDRESS)).isEqualTo(address);
        assertThat(ownerFormPage.getFieldValue(OwnerFormPage.CITY)).isEqualTo(city);
        assertThat(ownerFormPage.getFieldValue(OwnerFormPage.TELEPHONE)).isEqualTo(telephone);
    }

    @Test
    @DisplayName("Edit form updates fields and updated owner appears in list")
    void editOwnerUpdatesFields() {
        int ownerId = createOwner(firstName, lastName, address, city, telephone);

        String newFirstName = FAKER.name().firstName();
        String newLastName = FAKER.name().lastName() + FAKER.number().digits(6);
        String newCity = FAKER.address().city();

        OwnerDetailPage ownerDetailPage = openEditOwnerForm(ownerId)
            .fillField(OwnerFormPage.FIRST_NAME, newFirstName)
            .fillField(OwnerFormPage.LAST_NAME, newLastName)
            .fillField(OwnerFormPage.CITY, newCity)
            .submitForm();

        assertThat(ownerDetailPage.getOwnerName())
            .as("Updated owner should appear with new name")
            .contains(newFirstName + " " + newLastName);
    }

    private int createOwner(String firstName, String lastName,
                            String address, String city, String telephone) {
        return given().contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(Map.of(
                "firstName", firstName,
                "lastName",  lastName,
                "address",   address,
                "city",      city,
                "telephone", telephone
            ))
            .when()
            .post(OWNER_API)
            .then()
            .statusCode(201)
            .extract().path("id");
    }
}
