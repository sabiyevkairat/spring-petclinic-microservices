package org.springframework.samples.petclinic.api.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for all API tests.
 *
 * Configures REST Assured with:
 *  - Base URL from BASE_URL environment variable (defaults to http://localhost:8080)
 *  - JSON content type on all requests
 *  - Request and response logging for easier debugging
 *  - A shared Jackson ObjectMapper for deserialising response bodies
 *
 * All test classes extend this and can use the static `spec` field directly
 * in their given().spec(spec) calls.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseApiTest {

    protected static final Logger log = LoggerFactory.getLogger(BaseApiTest.class);

    /**
     * Shared request specification — sets base URL, content type, and logging.
     * Use as: given().spec(spec).when().get("/api/customer/owners")
     */
    protected RequestSpecification getSpec;
    protected RequestSpecification writeSpec;

    /**
     * Shared Jackson ObjectMapper with Java 8 date/time support.
     * Use to serialise request bodies or deserialise typed response POJOs.
     */
    protected final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    /**
     * Maximum acceptable response time in milliseconds for any single request.
     * Asserted in individual tests via: .time(lessThan(MAX_RESPONSE_TIME_MS))
     */
    protected static final long MAX_RESPONSE_TIME_MS = 2000L;

    @BeforeAll
    void setUpRestAssured() {
        String baseUrl = resolveBaseUrl();
        log.info("Running API tests against: {}", baseUrl);

        getSpec = new RequestSpecBuilder()
            .setBaseUri(baseUrl)
            .setAccept(ContentType.JSON)
            .addFilter(new RequestLoggingFilter())
            .addFilter(new ResponseLoggingFilter())
            .build();

        writeSpec = new RequestSpecBuilder()
            .setBaseUri(baseUrl)
            .setContentType(ContentType.JSON)
            .setAccept(ContentType.JSON)
            .addFilter(new RequestLoggingFilter())
            .addFilter(new ResponseLoggingFilter())
            .build();

        // Set global defaults (used when not using the spec builder)
        RestAssured.baseURI = baseUrl;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    /**
     * Resolves the base URL in the following order of precedence:
     *  1. BASE_URL environment variable (e.g. set in CI)
     *  2. base.url system property (e.g. -Dbase.url=http://staging:8080)
     *  3. Default: http://localhost:8080
     */
    private static String resolveBaseUrl() {
        String envUrl = System.getenv("BASE_URL");
        if (envUrl != null && !envUrl.isBlank()) {
            return envUrl;
        }
        String propUrl = System.getProperty("base.url");
        if (propUrl != null && !propUrl.isBlank()) {
            return propUrl;
        }
        return "http://localhost:8080";
    }
}
