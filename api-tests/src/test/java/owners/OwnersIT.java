package owners;

import base.BaseApiTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@DisplayName("Owners API")
public class OwnersIT extends BaseApiTest {
    protected static final String OWNERS_ENDPOINT = "/api/customer/owners";

    // Helper method to build owner
    private Map<String, Object> buildOwnerRequest(String firstName, String lastName) {
        return Map.of(
            "firstName", firstName,
            "lastName", lastName,
            "address", "123 Test Street",
            "city", "New York",
            "telephone", "1234567890"
        );
    }

    private int createOwner(String firstName, String lastName) {
        return given().spec(writeSpec)
                .body(buildOwnerRequest(firstName, lastName))
                .when()
                    .post(OWNERS_ENDPOINT)
                .then()
                    .statusCode(201)
                    .extract().path("id");
    }

    @Test
    @DisplayName("POST /owners creates owner and returns 201 with body")
    void createOwnerReturns201() {
        String firstName = "George";
        String lastName = "Franklin";

        given().spec(writeSpec)
            .body(buildOwnerRequest(firstName, lastName))
            .when()
            .post(OWNERS_ENDPOINT)
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("firstName", equalTo(firstName))
            .body("lastName", equalTo(lastName))
            .body("address", equalTo("123 Test Street"))
            .body("telephone", equalTo("1234567890"))
            .body("pets", notNullValue());
    }

    @Test
    @DisplayName("POST /owners with blank firstName returns 400")
    void createOwnerBlankFirstNameReturns400() {
        Map<String, Object> body = Map.of(
            "firstName", "",
            "lastName", "Franklin",
            "address",   "123 Test Street",
            "city",      "New York",
            "telephone", "1234567890"
        );

        given().spec(writeSpec)
            .body(body)
            .when()
                .post(OWNERS_ENDPOINT)
            .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("POST /owners with non-numeric telephone returns 400")
    void createOwnerNonNumericTelephoneReturns400() {
        Map<String, Object> body = Map.of(
            "firstName", "",
            "lastName", "Franklin",
            "address",   "123 Test Street",
            "city",      "New York",
            "telephone", "not-a-number"
        );

        given().spec(writeSpec)
            .body(body)
            .when()
                .post(OWNERS_ENDPOINT)
            .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("POST /owners with missing required fields returns 400")
    void createOwnerMissingRequiredFieldsReturns400() {

        given().spec(writeSpec)
            .body(Map.of())
            .when()
                .post(OWNERS_ENDPOINT)
            .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("GET /owners returns 200 with a list")
    void getAllOwnersReturns200() {
        given().spec(getSpec)
            .when()
            .get(OWNERS_ENDPOINT)
            .then()
            .statusCode(200)
            .body("$", instanceOf(List.class));
    }

    @Test
    @DisplayName("GET /owners response time is under 2000ms")
    void getAllOwnersResponseTimeIsAcceptable() {
        given().spec(getSpec)
            .when()
                .get(OWNERS_ENDPOINT)
            .then()
                .statusCode(200)
                .time(lessThan(MAX_RESPONSE_TIME_MS));
    }

    @Test
    @DisplayName("GET /owners each owner has required fields")
    void getAllOwnersHasCorrectSchema() {
        // ensure at least 1 owner exists
        createOwner("Schema", "Test");

        given().spec(getSpec)
            .when()
                .get(OWNERS_ENDPOINT)
            .then()
                .statusCode(200)
                .body("every { it.containsKey('id') }", is(true))
                .body("every { it.containsKey('firstName') }", is(true))
                .body("every { it.containsKey('lastName') }", is(true))
                .body("every { it.containsKey('pets') }", is(true));
    }

    @Test
    @DisplayName("GET /owners{id} returns the correct owner")
    void getOwnerByIdReturns200() {
        int ownerId = createOwner("John", "Doe");

        given().spec(getSpec)
            .when()
                .get(OWNERS_ENDPOINT + "/" + ownerId)
            .then()
                .statusCode(200)
                .body("id", equalTo(ownerId))
                .body("firstName", equalTo("John"))
                .body("lastName", equalTo("Doe"))
                .body("address", equalTo("123 Test Street"))
                .body("telephone", equalTo("1234567890"))
                .body("pets", notNullValue());
    }

    /**
     * Below test would require update to the @GetMapping(value = "/{ownerId}")
     * in OwnerResource.java which is currently not set to deal with
     * a scenario of a non-existent owner

    @Test
    @DisplayName("GET /owners{id} for non existent owner returns 404")
    void getOwnerByIdNotFoundReturns404() {
        given().spec(getSpec)
                .when()
                    .get(OWNERS_ENDPOINT + "/9999999")
                .then()
                    .statusCode(404);
    }
    */

    @Test
    @DisplayName("PUT /owners{id} updates owner and returns 204")
    void updateOwnerReturns204() {
        int ownerId = createOwner("Harold", "Smith");

        Map<String, Object> updatedBody = Map.of(
            "firstName", "Harold",
            "lastName",  "Updated",
            "address",   "456 New Avenue",
            "city",      "Boston",
            "telephone", "9876543210"
        );

        given().spec(writeSpec)
                .body(updatedBody)
                    .when()
                .put(OWNERS_ENDPOINT + "/" + ownerId)
                    .then()
                    .statusCode(204);

        // verify update persisted via a GET call
        given().spec(getSpec)
            .when()
                .get(OWNERS_ENDPOINT + "/" + ownerId)
            .then()
                .statusCode(200)
                .body("lastName",  equalTo("Updated"))
                .body("address",   equalTo("456 New Avenue"))
                .body("city",      equalTo("Boston"))
                .body("telephone", equalTo("9876543210"));
    }

    @Test
    @DisplayName("PUT /owners/{id} for non-existent owner returns 404")
    void updateNonExistentOwnerReturns404() {
        given().spec(writeSpec)
            .body(buildOwnerRequest("Ghost", "Owner"))
            .when()
                .put(OWNERS_ENDPOINT + "/999999")
            .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("PUT /owners/{id} with blank lastName returns 400")
    void updateOwnerBlankLastNameReturns400() {
        int id = createOwner("Validation", "Test");

        Map<String, Object> invalidBody = Map.of(
            "firstName", "Validation",
            "lastName",  "",
            "address",   "123 Test Street",
            "city",      "New York",
            "telephone", "1234567890"
        );

        given().spec(writeSpec)
            .body(invalidBody)
            .when()
                .put(OWNERS_ENDPOINT + "/" + id)
            .then()
                .statusCode(400);
    }
}
