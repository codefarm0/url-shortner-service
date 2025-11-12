# URL Shortener Service (Spring Boot)

A clean, layered URL shortener built with Spring Boot, JPA, Thymeleaf UI, and an optional K6 load test. It follows controller/service/repository architecture, uses records for DTOs, and includes a Snowflake-based ID generator with Base62 encoding.

## Requirements
- Java 25 (Gradle toolchains will auto-provision if not installed)
- Gradle Wrapper (included)
- Optional for load tests: k6

## Quick Start

### 1) Run the app
```bash
./gradlew clean bootRun
```
App starts at `http://localhost:8080` using in-memory H2.

H2 console (optional): `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:shortnerdb`
- Username: `sa`
- Password: (empty)

### 2) Use the Web UI
Open `http://localhost:8080/` and:
- Enter Long URL
- Optionally provide Custom Alias and User UUID
- Submit to get a short URL

### 3) Use the API
- Shorten a URL (optional `user_uuid` header):
```bash
curl -s -X POST 'http://localhost:8080/api/v1/shorten' \
  -H 'Content-Type: application/json' \
  -H 'user_uuid: user-123' \
  -d '{"longUrl":"https://example.com/very/long/path?param=value"}'
```
- Redirect (will return 301 with Location):
```bash
curl -I 'http://localhost:8080/{shortCode}'
```
- User metrics (count of shortened URLs per user):
```bash
curl -s 'http://localhost:8080/api/v1/metrics/users'
```

## Behavior & Decisions
- Redirects use HTTP 301 with headers:
  - `Cache-Control: private, max-age=90`
  - `X-Robots-Tag: noindex`
- Service expects optional `user_uuid` header; if present, it is stored on the mapping for metrics.
- DTOs are Java records (`ShortenRequest`, `ShortenResponse`).

## Load Testing with k6
A k6 script is provided to simulate read-heavy traffic with seeding.

### Script location
`load-test/url-shortener.k6.js`

### Default run (100 reads/sec, 1 write/sec, 1 min)
```bash
k6 run load-test/url-shortener.k6.js
```

### Tunable environment variables
- `BASE_URL` (default `http://localhost:8080`)
- `API_PATH` (default `/api/v1/shorten`)
- `DURATION` (default `1m`)
- `READ_RPS` (default `100`)
- `WRITE_RPS` (default `1`)
- `SEED_COUNT` (default `100`)
- `RANDOM_SEED` (default `12345`)

### Examples
- Increase duration and rates:
```bash
BASE_URL=http://localhost:8080 \
DURATION=2m \
READ_RPS=200 \
WRITE_RPS=2 \
k6 run load-test/url-shortener.k6.js
```
- Increase seed size:
```bash
SEED_COUNT=500 k6 run load-test/url-shortener.k6.js
```

The script:
- `setup()` seeds `SEED_COUNT` short URLs via POST.
- `redirects` scenario generates GET `/{shortCode}` requests (301 expected).
- `shorten` scenario generates POST requests to create new mappings.
- Checks verify status codes and the `Location` header on redirects.

## Config (application.properties)
- H2 is in-memory by default:
```
spring.jpa.hibernate.ddl-auto=update
spring.jpa.open-in-view=false
spring.h2.console.enabled=true
spring.thymeleaf.cache=false
spring.datasource.url=jdbc:h2:mem:shortnerdb;DB_CLOSE_DELAY=-1;MODE=MySQL
spring.datasource.username=sa
spring.datasource.password=
```
- Snowflake defaults (override via env/properties):
```
snowflake.datacenter.id=1
snowflake.machine.id=1
```

## Docs
- Class diagram: `docs/class-diagram.md`
- Sequence diagrams: `docs/sequence-diagram.md`

## Project Structure (high-level)
- `src/main/java/.../web` — Controllers (API, Web UI, Redirect)
- `src/main/java/.../core` — Service interface and implementation
- `src/main/java/.../model` — JPA entities
- `src/main/java/.../repository` — Spring Data JPA repository
- `src/main/java/.../util` — Snowflake + Base62 utilities
- `src/main/resources/templates` — Thymeleaf templates (UI)
- `load-test` — k6 script
- `docs` — Diagrams and documentation

## Notes
- For a fixed 6-character short code, map IDs into the 62^6 space and left-pad; current Base62 output length varies with magnitude (Snowflake → typically 10–11 chars over time). The service performs DB uniqueness checks on insert.
