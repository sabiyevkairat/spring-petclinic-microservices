package vets;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.samples.petclinic.api.base.BaseApiTest;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

@DisplayName("Vets API")
public class VetsIT extends BaseApiTest {
    protected static final String VETS_ENDPOINT = "/api/vet/vets";

    @Test
    @DisplayName("GET /api/vet/vets returns 200 with a non-empty list")
    void getAllVetsReturns200() {
        given().spec(getSpec)
            .when()
                .get(VETS_ENDPOINT)
            .then()
                .statusCode(200)
                .body("$", not(empty()));
    }

    @Test
    @DisplayName("GET /api/vet/vets response time is under 2000ms")
    void getAllVetsResponseTimeIsAcceptable() {
        given().spec(getSpec)
            .when()
                .get(VETS_ENDPOINT)
            .then()
                .statusCode(200)
                .time(lessThan(MAX_RESPONSE_TIME_MS));
    }

    @Test
    @DisplayName("GET /api/vet/vets each vet has required fields")
    void getAllVetsHasCorrectSchema() {
        given().spec(getSpec)
            .when()
                .get(VETS_ENDPOINT)
            .then()
                .statusCode(200)
                .body("every { it.containsKey('id') }", is(true))
                .body("every { it.containsKey('firstName') }", is(true))
                .body("every { it.containsKey('lastName') }", is(true))
                .body("every { it.containsKey('specialties') }", is(true))
                .body("every { it.containsKey('nrOfSpecialties') }", is(true));
    }

    @Test
    @DisplayName("GET /api/vet/vets specialties are well-formed objects")
    void getAllSpecialtiesAreWellFormed() {
        Response response = given().spec(getSpec)
            .when()
                .get(VETS_ENDPOINT)
            .then()
                .statusCode(200)
                .extract().response();

        JsonPath json = response.jsonPath();
        List<Map<String, Object>> vets = json.getList("$");

        // Find vets that have at least one specialty and validate the structure
        List<Map<String, Object>> vetsWithSpecialties = vets.stream()
            .filter(vet -> {
                List<?> specialties = (List<?>) vet.get("specialties");
                return specialties != null && !specialties.isEmpty();
            })
            .toList();

        assertThat(vetsWithSpecialties)
            .as("Expected at least one vet with specialties in the response")
            .isNotEmpty();

        vetsWithSpecialties.forEach(vet -> {
            List<Map<String, Object>> specialties = (List<Map<String, Object>>) vet.get("specialties");
            specialties.forEach(specialty -> {
                assertThat(specialty).containsKey("id");
                assertThat(specialty).containsKey("name");
                assertThat(specialty.get("name")).isNotNull();
            });
        });
    }
}
