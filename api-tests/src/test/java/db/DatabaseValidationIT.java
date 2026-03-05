package db;

import base.BaseApiTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * API-10 — MySQL persistence validation tests.
 *
 * Verifies that POST operations persist data correctly to MySQL by:
 *   1. Creating a resource via the API
 *   2. Querying MySQL directly via DatabaseHelper
 *   3. Asserting the row matches the request payload
 *
 * Requires the stack to be running with the MySQL Spring profile active:
 *   docker compose -f docker-compose.yml -f docker-compose.test.yml up -d
 *
 * These tests are tagged @Tag("db-validation") and can be run independently:
 *   ./mvnw verify -pl api-tests -Dgroups=db-validation
 */
@Tag("db-validation")
@DisplayName("MySQL Persistence Validation")
class DatabaseValidationIT extends BaseApiTest {

    private static final String OWNERS_ENDPOINT = "/api/customer/owners";
    private static final String VISITS_ENDPOINT = "/api/visit";

    // ── Owners ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Owners")
    class Owners {

        @Test
        @DisplayName("POST /owners persists all fields correctly to MySQL")
        void createOwnerPersistsToDatabase() throws Exception {
            // Create via API
            int id = given().spec(writeSpec)
                .body(Map.of(
                    "firstName", "Database",
                    "lastName",  "Validation",
                    "address",   "99 MySQL Street",
                    "city",      "DataCity",
                    "telephone", "5551234567"
                ))
                .when()
                    .post(OWNERS_ENDPOINT)
                .then()
                    .statusCode(201)
                    .extract().path("id");

            // Verify directly in MySQL
            try (DatabaseHelper db = new DatabaseHelper()) {
                Map<String, Object> row = db.findOwnerById(id);

                assertThat(row).isNotNull();
                assertThat(row.get("first_name")).isEqualTo("Database");
                assertThat(row.get("last_name")).isEqualTo("Validation");
                assertThat(row.get("address")).isEqualTo("99 MySQL Street");
                assertThat(row.get("city")).isEqualTo("DataCity");
                assertThat(row.get("telephone")).isEqualTo("5551234567");
            }
        }

        @Test
        @DisplayName("POST /owners assigns a unique generated ID in MySQL")
        void createOwnerGeneratesUniqueId() throws Exception {
            int firstId = given().spec(writeSpec)
                .body(Map.of(
                    "firstName", "First",
                    "lastName",  "Owner",
                    "address",   "1 First Street",
                    "city",      "City",
                    "telephone", "1111111111"
                ))
                .when()
                    .post(OWNERS_ENDPOINT)
                .then()
                    .statusCode(201)
                    .extract().path("id");

            int secondId = given().spec(writeSpec)
                .body(Map.of(
                    "firstName", "Second",
                    "lastName",  "Owner",
                    "address",   "2 Second Street",
                    "city",      "City",
                    "telephone", "2222222222"
                ))
                .when()
                    .post(OWNERS_ENDPOINT)
                .then()
                    .statusCode(201)
                    .extract().path("id");

            assertThat(firstId).isNotEqualTo(secondId);

            try (DatabaseHelper db = new DatabaseHelper()) {
                assertThat(db.findOwnerById(firstId)).isNotNull();
                assertThat(db.findOwnerById(secondId)).isNotNull();
            }
        }
    }

    // ── Pets ──────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Pets")
    class Pets {

        @Test
        @DisplayName("POST /owners/{ownerId}/pets persists all fields correctly to MySQL")
        void createPetPersistsToDatabase() throws Exception {
            // Create owner first
            int ownerId = given().spec(writeSpec)
                .body(Map.of(
                    "firstName", "Pet",
                    "lastName",  "DbOwner",
                    "address",   "10 Pet Lane",
                    "city",      "PetCity",
                    "telephone", "3333333333"
                ))
                .when()
                    .post(OWNERS_ENDPOINT)
                .then()
                    .statusCode(201)
                    .extract().path("id");

            // Create pet via API
            int petId = given().spec(writeSpec)
                .body(Map.of(
                    "name",      "DbPet",
                    "birthDate", "2021-05-20",
                    "typeId",    1
                ))
                .when()
                    .post(OWNERS_ENDPOINT + "/" + ownerId + "/pets")
                .then()
                    .statusCode(201)
                    .extract().path("id");

            // Verify in MySQL
            try (DatabaseHelper db = new DatabaseHelper()) {
                Map<String, Object> row = db.findPetById(petId);

                assertThat(row).isNotNull();
                assertThat(row.get("name")).isEqualTo("DbPet");
                assertThat(row.get("owner_id")).isEqualTo((long) ownerId);
                assertThat(row.get("type_id")).isEqualTo((long) 1);
            }
        }

        @Test
        @DisplayName("POST /owners/{ownerId}/pets links pet to correct owner in MySQL")
        void createPetLinksToOwnerInDatabase() throws Exception {
            // Create owner
            int ownerId = given().spec(writeSpec)
                .body(Map.of(
                    "firstName", "Link",
                    "lastName",  "TestOwner",
                    "address",   "5 Link Road",
                    "city",      "LinkCity",
                    "telephone", "4444444444"
                ))
                .when()
                    .post(OWNERS_ENDPOINT)
                .then()
                    .statusCode(201)
                    .extract().path("id");

            // Create two pets for the same owner
            given().spec(writeSpec)
                .body(Map.of("name", "PetOne", "birthDate", "2020-01-01", "typeId", 1))
                .when()
                    .post(OWNERS_ENDPOINT + "/" + ownerId + "/pets")
                .then()
                    .statusCode(201);

            given().spec(writeSpec)
                .body(Map.of("name", "PetTwo", "birthDate", "2020-02-01", "typeId", 2))
                .when()
                    .post(OWNERS_ENDPOINT + "/" + ownerId + "/pets")
                .then()
                    .statusCode(201);

            // Verify both pets are linked to the owner in MySQL
            try (DatabaseHelper db = new DatabaseHelper()) {
                List<Map<String, Object>> pets = db.findPetsByOwnerId(ownerId);

                assertThat(pets).hasSize(2);
                assertThat(pets).extracting(p -> p.get("name"))
                    .containsExactlyInAnyOrder("PetOne", "PetTwo");
                assertThat(pets).allMatch(p ->
                    Long.valueOf(ownerId).equals(p.get("owner_id")));
            }
        }
    }

    // ── Visits ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Visits")
    class Visits {

        @Test
        @DisplayName("POST /owners/*/pets/{petId}/visits persists all fields correctly to MySQL")
        void createVisitPersistsToDatabase() throws Exception {
            // Create owner and pet
            int ownerId = given().spec(writeSpec)
                .body(Map.of(
                    "firstName", "Visit",
                    "lastName",  "DbOwner",
                    "address",   "20 Visit Ave",
                    "city",      "VisitCity",
                    "telephone", "5555555555"
                ))
                .when()
                    .post(OWNERS_ENDPOINT)
                .then()
                .statusCode(201)
                    .extract().path("id");

            int petId = given().spec(writeSpec)
                .body(Map.of("name", "VisitPet", "birthDate", "2022-03-10", "typeId", 2))
                .when()
                    .post(OWNERS_ENDPOINT + "/" + ownerId + "/pets")
                .then()
                    .statusCode(201)
                    .extract().path("id");

            // Create visit via API
            int visitId = given().spec(writeSpec)
                .body(Map.of(
                    "date",        "2024-07-15",
                    "description", "DB validation visit"
                ))
                .when()
                    .post(VISITS_ENDPOINT + "/owners/*/pets/" + petId + "/visits")
                .then()
                    .statusCode(201)
                    .extract().path("id");

            // Verify in MySQL
            try (DatabaseHelper db = new DatabaseHelper()) {
                Map<String, Object> row = db.findVisitById(visitId);

                assertThat(row).isNotNull();
                assertThat(row.get("description")).isEqualTo("DB validation visit");
                assertThat(row.get("pet_id")).isEqualTo((long) petId);
                assertThat(row.get("visit_date")).isNotNull();
            }
        }

        @Test
        @DisplayName("POST multiple visits for same pet all persist to MySQL")
        void multipleVisitsForSamePetAllPersist() throws Exception {
            // Create owner and pet
            int ownerId = given().spec(writeSpec)
                .body(Map.of(
                    "firstName", "Multi",
                    "lastName",  "VisitOwner",
                    "address",   "30 Multi Road",
                    "city",      "MultiCity",
                    "telephone", "6666666666"
                ))
                .when()
                    .post(OWNERS_ENDPOINT)
                .then()
                    .statusCode(201)
                    .extract().path("id");

            int petId = given().spec(writeSpec)
                .body(Map.of("name", "MultiVisitPet", "birthDate", "2021-06-01", "typeId", 3))
                .when()
                .post(OWNERS_ENDPOINT + "/" + ownerId + "/pets")
                .then()
                .statusCode(201)
                .extract().path("id");

            String visitsEndpoint = VISITS_ENDPOINT + "/owners/*/pets/" + petId + "/visits";

            // Create three visits
            given().spec(writeSpec)
                .body(Map.of("date", "2024-01-10", "description", "Visit one"))
                .when().post(visitsEndpoint).then().statusCode(201);

            given().spec(writeSpec)
                .body(Map.of("date", "2024-02-10", "description", "Visit two"))
                .when().post(visitsEndpoint).then().statusCode(201);

            given().spec(writeSpec)
                .body(Map.of("date", "2024-03-10", "description", "Visit three"))
                .when().post(visitsEndpoint).then().statusCode(201);

            // Verify all three are in MySQL
            try (DatabaseHelper db = new DatabaseHelper()) {
                List<Map<String, Object>> visits = db.findVisitsByPetId(petId);

                assertThat(visits).hasSize(3);
                assertThat(visits).extracting(v -> v.get("description"))
                    .containsExactlyInAnyOrder("Visit one", "Visit two", "Visit three");
                assertThat(visits).allMatch(v ->
                    Long.valueOf(petId).equals(v.get("pet_id")));
            }
        }
    }
}
