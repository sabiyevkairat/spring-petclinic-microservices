package errors;

import base.BaseApiTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@DisplayName("Error Handling and Negative Paths")
public class ErrorHandlingIT extends BaseApiTest {

    private static final String OWNERS_ENDPOINT = "/api/customer/owners";
    private static final String VETS_ENDPOINT   = "/api/vet/vets";
    private static final String VISITS_ENDPOINT = "/api/visit";

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

    private int createOwner() {
        return given().spec(writeSpec)
            .body(Map.of(
                "firstName", "Error",
                "lastName",  "TestOwner",
                "address",   "123 Test Street",
                "city",      "New York",
                "telephone", "1234567890"
            ))
            .when()
            .post(OWNERS_ENDPOINT)
            .then()
            .statusCode(201)
            .extract().path("id");
    }

    @Nested
    @DisplayName("Owners")
    class Owners {

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

        /*
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
            int id = createOwner();

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

    @Nested
    @DisplayName("Pets")
    class Pets {

        @Test
        @DisplayName("GET /owners/*/pets/{petId} non-existent pet returns 404")
        void getNonExistentPetReturns404() {
            given().spec(getSpec)
                    .when()
                        .get(OWNERS_ENDPOINT + "/*/pets/999999")
                    .then()
                        .statusCode(404);
        }

        @Test
        @DisplayName("POST /owners/{ownerId}/pets for non-existent owner returns 404")
        void createPetForNonExistentOwnerReturns404() {
            given().spec(writeSpec)
                .body(Map.of(
                    "name",      "Fluffy",
                    "birthDate", "2020-01-01",
                    "typeId",    1
                ))
                .when()
                    .post(OWNERS_ENDPOINT + "/999999/pets")
                .then()
                    .statusCode(404)
                    .body("status", equalTo(404))
                    .body("error",  equalTo("Not Found"));
        }

        @Test
        @DisplayName("POST /owners/{ownerId}/pets with an invalid owner id (0) returns 400")
        void createPetWithInvalidOwnerIdReturns400() {
            given().spec(writeSpec)
                .body(Map.of())
                .when()
                .post(OWNERS_ENDPOINT + "/0/pets")
                .then()
                .statusCode(400);
        }
    }

    @Nested
    @DisplayName("Visits")
    class Visits {

        @Test
        @DisplayName("GET /pets/visits without petId param returns 400")
        void getVisitsWithoutPetIdReturns400() {
            given().spec(getSpec)
                    .when()
                        .get(VISITS_ENDPOINT + "/pets/visits")
                    .then()
                        .statusCode(400);
        }

        @Test
        @DisplayName("POST /owners/*/pets/{petId}/visits with petId 0 returns 400")
        void createVisitWithPetId0Returns400() {
            given().spec(writeSpec)
                    .body(Map.of(
                            "visitDate", "2022-01-01",
                            "description", "Regular checkup"
                    ))
                    .when()
                        .post(VISITS_ENDPOINT + "/owners/*/pets/0/visits")
                    .then()
                        .statusCode(400);
        }
    }

    @Nested
    @DisplayName("Vets")
    class Vets {

        @Test
        @DisplayName("GET /vets with unsupported method (POST) returns 405")
        void postToVetsReturns200() {
            given().spec(writeSpec)
                    .when()
                        .post(VETS_ENDPOINT)
                    .then()
                        .statusCode(405);
        }
    }
}
