# spring-petclinic-api-tests

REST API test suite for [Spring PetClinic Microservices](https://github.com/spring-petclinic/spring-petclinic-microservices).

Built with **REST Assured 5**, **JUnit 5**, **AssertJ**, and **Jackson**.

---

## Project Structure

```
spring-petclinic-microservices/
├── api-tests/                               ← this module
│   ├── src/test/java/
│   │   ├── base/
│   │   │   ├── BaseApiTest.java             # Shared specs, base URL resolution
│   │   │   └── SmokeIT.java                 # Gateway reachability smoke tests
│   │   ├── owners/                          # Owner CRUD tests (API-02)
│   │   ├── pets/                            # Pet CRUD tests (API-03)
│   │   ├── vets/                            # Vets read tests (API-04)
│   │   └── visits/                          # Visit tests (API-05)
│   ├── config/
│   │   ├── checkstyle.xml                   # Checkstyle rules
│   │   ├── checkstyle-suppressions.xml      # Checkstyle suppressions for test code
│   │   └── spotbugs-exclude.xml             # SpotBugs false positive exclusions
│   ├── pom.xml
│   └── README.md
├── run-api-tests.sh                         # One-command test runner
└── docker-compose.yml
```

---

## Prerequisites

- Java 17+
- Maven 3.9+
- Docker + Docker Compose

---

## Quickstart — One Command

The easiest way to run everything. From the repo root:

```bash
./run-api-tests.sh
```

This single script will:
1. Start all PetClinic containers via Docker Compose
2. Wait until the API Gateway is healthy (polls up to 120s)
3. Run the full API test suite
4. Shut down all containers — even if tests fail

> **Note:** The first run requires Docker images to be built. If you haven't done this yet, run the build step below first.

---

## First-Time Setup

Docker images need to be built once before you can run the stack:

```bash
./mvnw clean install -P buildDocker -DskipTests
```

This takes a few minutes. Once done, `run-api-tests.sh` handles everything else on subsequent runs.

---

## Running Tests Manually

If you prefer to manage the stack yourself:

```bash
# Start the stack
docker compose up -d

# Wait until the gateway is ready — check Eureka at http://localhost:8761
# All services should appear as UP before running tests

# Run all API tests from the repo root
./mvnw verify -pl api-tests

# Run against a different environment
BASE_URL=http://staging.example.com:8080 ./mvnw verify -pl api-tests

# Run only smoke tests
./mvnw verify -pl api-tests -Dit.test=SmokeIT

# Run only tests tagged as e2e
./mvnw verify -pl api-tests -Dgroups=e2e

# Shut down when done
docker compose down
```

---

## Architecture Under Test

All tests target the **API Gateway** on port 8080, which routes to downstream services.
The `StripPrefix=2` filter on each route strips the first two path segments before
forwarding, so the paths below are what your tests use — not the internal service paths.

| Service           | Gateway Prefix     | Example endpoint                              |
|-------------------|--------------------|-----------------------------------------------|
| Customers Service | `/api/customer/**` | `GET /api/customer/owners`                    |
| Vets Service      | `/api/vet/**`      | `GET /api/vet/vets`                           |
| Visits Service    | `/api/visit/**`    | `GET /api/visit/owners/*/pets/{petId}/visits` |

### Visits Service — Actual Endpoints

The visits controller exposes three endpoints (no top-level `/visits` route):

| Method | Path                                      | Description                      |
|--------|-------------------------------------------|----------------------------------|
| POST   | `/api/visit/owners/*/pets/{petId}/visits` | Create a visit for a pet         |
| GET    | `/api/visit/owners/*/pets/{petId}/visits` | Get visits for a specific pet    |
| GET    | `/api/visit/pets/visits?petId=1,2,3`      | Bulk visit lookup by pet ID list |

> The bulk endpoint (`pets/visits`) requires at least one `petId` query parameter —
> calling it without one returns `400 Bad Request`.

---

## Configuration

| Variable    | Default                   | How to set                                         |
|-------------|---------------------------|----------------------------------------------------|
| `BASE_URL`  | `http://localhost:8080`   | Env var: `BASE_URL=http://host:8080 ./mvnw verify` |
| `base.url`  | `http://localhost:8080`   | System property: `./mvnw verify -Dbase.url=...`    |

Resolution order: `BASE_URL` env var → `-Dbase.url` system property → default.

---

## Request Specifications

`BaseApiTest` exposes two REST Assured specs to use in tests:

| Spec        | Sets                                        | Use for              |
|-------------|---------------------------------------------|----------------------|
| `getSpec`   | `Accept: application/json`                  | GET requests         |
| `writeSpec` | `Accept` + `Content-Type: application/json` | POST, PUT, DELETE    |

Always use `getSpec` for read operations — sending `Content-Type` on a GET request
causes some endpoints to return `400 Bad Request`.

---

## Groovy Compatibility Note

This module inherits Spring Boot 4 as parent, which bundles a version of Groovy that
conflicts with REST Assured's internal HTTP engine. The `pom.xml` explicitly excludes
and re-pins Groovy 4.0.21 to resolve this. Do not remove these exclusions.

---

## Naming Conventions

| Pattern                 | Meaning                                                   |
|-------------------------|-----------------------------------------------------------|
| `*IT.java`              | Integration test — picked up by Failsafe on `mvn verify`  |
| `@Tag("e2e")`           | Cross-service end-to-end scenario test                    |
| `@Tag("db-validation")` | Tests that validate database state directly via JDBC      |

---

## Starting the PetClinic Stack

From the root of the `spring-petclinic-microservices` repo:

```bash
# Build Docker images (first time only, takes a few minutes)
./mvnw clean install -P buildDocker -DskipTests

# Start all services
docker compose up -d

# Watch startup — wait until api-gateway is healthy
docker compose logs -f api-gateway
```

Once you see the gateway log `Started ApiGatewayApplication`, the stack is ready.
You can also check the Eureka dashboard at http://localhost:8761 to confirm all
services are registered.

---

## Code Quality

Checkstyle and SpotBugs run automatically as part of `mvn verify`. Both tools fail
the build on violations so issues are caught before code is merged.

### Checkstyle

Enforces Google Java Style Guide with the following relaxations for test code:

- Line length extended to 150 chars to accommodate REST Assured fluent chains
- `*IT.java` files have line length suppressed entirely
- `@Test` and JUnit lifecycle methods (`@BeforeAll`, `@AfterAll` etc.) are exempt from Javadoc

Configuration: `config/checkstyle.xml`
Suppressions: `config/checkstyle-suppressions.xml`

### SpotBugs

Performs static analysis at `Max` effort with `Medium` threshold. The following
false positives common in REST Assured test code are excluded:

- `UNCHECKED_CAST` — unavoidable when extracting typed data from `JsonPath.getList()`
- `ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD` — standard JUnit 5 `@BeforeAll` pattern
- `DMI_HARDCODED_ABSOLUTE_FILENAME` — API path strings in test bodies

Exclusions: `config/spotbugs-exclude.xml`

### Running linting without tests

```bash
# Checkstyle only
./mvnw checkstyle:check -pl api-tests

# SpotBugs only
./mvnw spotbugs:check -pl api-tests

# Both tools + tests (normal workflow)
./mvnw verify -pl api-tests
```

---

## Test Reports

After running `mvn verify`, reports are available at:

```
target/failsafe-reports/     # XML reports (used by CI)
target/surefire-reports/     # Additional Surefire output
```

---

## Configuration

| Variable       | Default                 | Description                        |
|----------------|-------------------------|------------------------------------|
| `BASE_URL`     | `http://localhost:8080` | Set as env var for CI environments |
| `base.url`     | `http://localhost:8080` | Alternative system property        |

---

## Naming Conventions

| Pattern   | Meaning                                      |
|-----------|----------------------------------------------|
| `*IT.java`| Integration test — run by Failsafe via `mvn verify` |
| `@Tag("e2e")` | End-to-end cross-service tests          |
| `@Tag("db-validation")` | Tests that validate DB state via JDBC |
