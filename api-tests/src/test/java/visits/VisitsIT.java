package visits;

import base.BaseApiTest;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

@DisplayName("Visits API")
class VisitsIT extends BaseApiTest {

    private static final String CUSTOMERS_ENDPOINT = "/api/customer/owners";
    private static final String VISITS_ENDPOINT    = "/api/visit";

    private int petId;

    @BeforeEach
    void createOwnerAndPet() {
        int ownerId = given().spec(writeSpec)
            .body(Map.of(
                "firstName", "Visit",
                "lastName",  "TestOwner",
                "address",   "123 Test Street",
                "city",      "New York",
                "telephone", "1234567890"
            ))
            .when()
                .post(CUSTOMERS_ENDPOINT)
            .then()
                .statusCode(201)
                .extract().path("id");

        petId = given().spec(writeSpec)
            .body(Map.of(
                "name",      "TestPet",
                "birthDate", "2020-01-01",
                "typeId",    1
            ))
            .when()
                .post(CUSTOMERS_ENDPOINT + "/" + ownerId + "/pets")
            .then()
                .statusCode(201)
                .extract().path("id");
    }

    private Map<String, Object> buildVisitRequest(String description) {
        return Map.of(
            "date",        "2024-06-15",
            "description", description
        );
    }

    private int createVisit(String description) {
        return given().spec(writeSpec)
            .body(buildVisitRequest(description))
            .when()
                .post(visitsPath())
            .then()
                .statusCode(201)
                .extract().path("id");
    }

    private String visitsPath() {
        return VISITS_ENDPOINT + "/owners/*/pets/" + petId + "/visits";
    }

    @Test
    @DisplayName("POST /owners/*/pets/{petId}/visits creates visit and returns 201")
    void createVisitReturns201() {
        given().spec(writeSpec)
            .body(buildVisitRequest("Annual checkup"))
            .when()
                .post(visitsPath())
            .then()
                .statusCode(201)
                .body("id",          notNullValue())
                .body("date",        notNullValue())
                .body("description", equalTo("Annual checkup"))
                .body("petId",       equalTo(petId));
    }

    @Test
    @DisplayName("POST /owners/*/pets/{petId}/visits without date uses current date")
    void createVisitWithoutDateUsesDefault() {
        // date defaults to new Date() in Visit model — omitting it should still return 201
        given().spec(writeSpec)
            .body(Map.of("description", "No date supplied"))
            .when()
                .post(visitsPath())
            .then()
                .statusCode(201)
                .body("id",   notNullValue())
                .body("date", notNullValue());
    }

    @Test
    @DisplayName("POST /owners/*/pets/{petId}/visits without description returns 201")
    void createVisitWithoutDescriptionReturns201() {
        // description is nullable — omitting it is valid
        given().spec(writeSpec)
            .body(Map.of("date", "2024-06-15"))
            .when()
                .post(visitsPath())
            .then()
                .statusCode(201)
                .body("id", notNullValue());
    }

    @Test
    @DisplayName("POST /owners/*/pets/{petId}/visits response time is under 2000ms")
    void createVisitResponseTimeIsAcceptable() {
        given().spec(writeSpec)
            .body(buildVisitRequest("Response time check"))
            .when()
                .post(visitsPath())
            .then()
                .statusCode(201)
                .time(lessThan(MAX_RESPONSE_TIME_MS));
    }

    @Test
    @DisplayName("GET /owners/*/pets/{petId}/visits returns list of visits for pet")
    void getVisitsByPetIdReturns200() {
        createVisit("First visit");
        createVisit("Second visit");

        given().spec(getSpec)
            .when()
                .get(visitsPath())
            .then()
                .statusCode(200)
                .body("$",           not(empty()))
                .body("description", hasItems("First visit", "Second visit"))
                .body("petId",       everyItem(equalTo(petId)));
    }

    @Test
    @DisplayName("GET /owners/*/pets/{petId}/visits for pet with no visits returns empty list")
    void getVisitsByPetIdWithNoVisitsReturnsEmptyList() {
        // Fresh pet created in @BeforeEach has no visits yet
        given().spec(getSpec)
            .when()
                .get(visitsPath())
            .then()
                .statusCode(200)
                .body("$", empty());
    }

    @Test
    @DisplayName("GET /owners/*/pets/{petId}/visits each visit has required fields")
    void getVisitsByPetIdHasCorrectSchema() {
        createVisit("Schema check visit");

        given().spec(getSpec)
            .when()
                .get(visitsPath())
            .then()
                .statusCode(200)
                .body("every { it.containsKey('id') }",          is(true))
                .body("every { it.containsKey('date') }",        is(true))
                .body("every { it.containsKey('petId') }",       is(true));
    }

    @Test
    @DisplayName("GET /owners/*/pets/{petId}/visits response time is under 2000ms")
    void getVisitsByPetIdResponseTimeIsAcceptable() {
        given().spec(getSpec)
            .when()
                .get(visitsPath())
            .then()
                .statusCode(200)
                .time(lessThan(MAX_RESPONSE_TIME_MS));
    }

    @Test
    @DisplayName("GET /pets/visits?petId returns wrapped items list")
    void bulkGetVisitsByPetIdReturns200() {
        createVisit("Bulk lookup visit");

        given().spec(getSpec)
            .queryParam("petId", petId)
            .when()
                .get(VISITS_ENDPOINT + "/pets/visits")
            .then()
                .statusCode(200)
                .body("items",             not(empty()))
                .body("items.description", hasItem("Bulk lookup visit"))
                .body("items.petId",       everyItem(equalTo(petId)));
    }

    @Test
    @DisplayName("GET /pets/visits?petId returns correct wrapper structure")
    void bulkGetVisitsHasWrappedStructure() {
        // Response is { "items": [...] } not a plain array
        Response response = given().spec(getSpec)
            .queryParam("petId", petId)
            .when()
                .get(VISITS_ENDPOINT + "/pets/visits")
            .then()
                .statusCode(200)
                .extract().response();

        Object itemsValue = response.jsonPath().get("items");
        Object bodyValue = response.jsonPath().get("$");
        assertThat(itemsValue).isNotNull();
        assertThat(bodyValue).isInstanceOf(Map.class);
    }

    @Test
    @DisplayName("GET /pets/visits with multiple petIds returns visits for all pets")
    void bulkGetVisitsForMultiplePetsReturnsAll() {
        // Create a second owner+pet and visit to test multi-petId lookup
        int secondOwnerId = given().spec(writeSpec)
            .body(Map.of(
                "firstName", "Second",
                "lastName",  "Owner",
                "address",   "456 Other Street",
                "city",      "Boston",
                "telephone", "9876543210"
            ))
            .when()
                .post(CUSTOMERS_ENDPOINT)
            .then()
                .statusCode(201)
                .extract().path("id");

        int secondPetId = given().spec(writeSpec)
            .body(Map.of(
                "name",      "SecondPet",
                "birthDate", "2021-01-01",
                "typeId",    2
            ))
            .when()
                .post(CUSTOMERS_ENDPOINT + "/" + secondOwnerId + "/pets")
            .then()
                .statusCode(201)
                .extract().path("id");

        // Create a visit for each pet
        createVisit("First pet visit");
        given().spec(writeSpec)
            .body(buildVisitRequest("Second pet visit"))
            .when()
                .post(VISITS_ENDPOINT + "/owners/*/pets/" + secondPetId + "/visits")
            .then()
                .statusCode(201);

        // Bulk lookup for both pets
        Response response = given().spec(getSpec)
            .queryParam("petId", petId)
            .queryParam("petId", secondPetId)
            .when()
                .get(VISITS_ENDPOINT + "/pets/visits")
            .then()
                .statusCode(200)
                .extract().response();

        List<Map<String, Object>> items = response.jsonPath().getList("items");
        assertThat(items).hasSizeGreaterThanOrEqualTo(2);

        List<Integer> returnedPetIds = response.jsonPath().getList("items.petId");
        assertThat(returnedPetIds).contains(petId, secondPetId);
    }
}
