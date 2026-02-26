package base;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Smoke tests — verify the API Gateway is reachable and core routes respond.
 * These run first and fast. If these fail, there is no point running the full suite.
 */
@DisplayName("Smoke Tests")
class SmokeIT extends BaseApiTest {

    @Test
    @DisplayName("API Gateway is reachable and owners endpoint returns 200")
    void apiGatewayIsReachable() {
        given().spec(getSpec)
            .when()
                .get("/api/customer/owners")
            .then()
                .statusCode(200)
                .body(notNullValue())
                .time(lessThan(MAX_RESPONSE_TIME_MS));
    }

    @Test
    @DisplayName("Vets endpoint is reachable and returns 200")
    void vetsEndpointIsReachable() {
        given().spec(getSpec)
            .when()
                .get("/api/vet/vets")
            .then()
                .statusCode(200)
                .body(notNullValue())
                .time(lessThan(MAX_RESPONSE_TIME_MS));
    }

    @Test
    @DisplayName("Visits endpoint is reachable and returns 200")
    void visitsEndpointIsReachable() {
        given().spec(getSpec)
            .when()
            .get("/api/visit/owners/*/pets/1/visits")
            .then()
            .statusCode(200)
            .time(lessThan(MAX_RESPONSE_TIME_MS));
    }
}
