# Insurance Claims Processing API

[![CI](https://github.com/mbecht/claims-api/actions/workflows/ci.yml/badge.svg)](https://github.com/mbecht/claims-api/actions/workflows/ci.yml)

This personal project is a demonstration of a Spring Boot REST API that could be used for enterprise insurance software needs.
The backend service takes a mock insurance claim from submission to payout, enforcing business rules, role-based permissions and a full audit trail.

> **Status:** In progress - Story 4 of 17 complete

## What this project demonstrates
- Domain modeling
- Transactional business logic
- Security and layered architecture
- Production style testing

## Tech stack
Java 25 · Spring Boot 4.1 · Maven · PostgreSQL 17 · Docker Compose · JUnit 5 · Testcontainers · GitHub Actions

## Getting started

**Prerequisites:** JDK 25, Docker Desktop

1. Clone the repository
2. Copy the environment template and set a password
3. Start the app and database
```bash
   docker compose up --build
```
4. Check that it's running
```bash
   curl http://localhost:8080/actuator/health
   # {"status":"UP"}
```

## Local development accounts

> [!WARNING]
> These accounts are created by dev-only seed data and exist **only in your local Docker database**. They are never loaded in tests or production.

| Username | Role | Used to test |
|---|---|---|
| 'alice.holder' | Policyholder | Submitting and viewing her own claims |
| 'bob.holder' | Policyholder | Access control test |
| 'carol.adjuster' | Adjuster | Reviewing claims up to $10,000 |
| 'dave.supervisor' | Supervisor | Approving claims over $10,000 |

Seed data lives in 'src/main/resources/db/dev-data/' and runs only when the 'dev' profile is active.

## Running tests
```bash
./mvnw verify
```
Docker must be running; tests start a temporary Postgres container.

## API

With Docker Desktop running, boot app wired to a Testcontainers Postgres via TestcontainersConfiguration:

```powershell
$env:SPRING_PROFILES_ACTIVE = "dev"
  ./mvnw.cmd spring-boot:test-run
```

The following POST body ([requests/submit-claim.json](requests/submit-claim.json)) references seed data in the dev profile:

```json
{
  "policyNumber": 1001,
  "incidentDate": "2026-09-20T09:15:00",
  "amount": 1250.00,
  "description": "Rear bumper damage from parking lot collision"
}
```

The following POST request writes to a differente Postgres instsance than Docker. The difference is a persistent claims DB vs. a ephemeral Testcontainers test DB.

```bash
curl.exe -i -X POST http://localhost:8080/api/claims `
 -H "Content-Type: application/json" `
 -d "requests\submit-claim.json"
```

A valid POST will print response headers with 201 status and a body that verifies "status":"SUBMITTED". The claims table on same Postgres instance will record the new claim record.

## Error responses

All errors return a consistent JSON body following the [RFC 9457 Problem Details](https://www.rfc-editor.org/rfc/rfc9457) standard, with the content type of 'application/problem+json'. Every response includes a 'errorCode' as well as a UTC 'timestamp'.

| Error code | HTTP status | Meaning | Example cause |
|---|:---:|---|---|
| `VALIDATION_FAILED` | 400 | One or more fields are invalid; see `errors` | Negative claim amount |
| `MALFORMED_REQUEST` | 400 | The request body isn't valid JSON or has the wrong types | Text sent where a number is expected |
| `CLAIM_NOT_FOUND` | 404 | The requested item or URL doesn't exist | Unknown claim ID |
| `ILLEGAL_STATUS_TRANSITION` | 409 | The request conflicts with the item's current state | Moving a claim from Submitted straight to Paid |
| `BUSINESS_RULE_VIOLATION` | 422 | The request is valid but breaks a business rule | Filing a claim on an expired policy |
| `INTERNAL_ERROR` | 500 | Unexpected server error | Database unavailable |

> [!NOTE]
> For security, `500` responses return only a generic message. Details such as
> stack traces, class names, and SQL are logged on the server and never sent to
> the client. Validation errors also omit the rejected values, since they may
> contain personal data.

## Architecture
The app will run in one container with Postgres in another, connected by Docker Compose.

## Design decisions
See [docs/decisions](docs/decisions/) for the reasoning behind key choices.

## Roadmap
- [x] Project skeleton, Docker, CI
- [x] Database schema and migrations
- [x] Consistent error responses
- [x] Claim submission
- [ ] Coverage validation
- [ ] View and search claims
- [ ] Login with token
- [ ] Role-based access
- [ ] Enforce the claim lifecycle
- [ ] Adjuster reviews
- [ ] Escalate to supervisor
- [ ] Record payment
- [ ] Flag suspicious claims
- [ ] Audit trail
- [ ] API docs
- [ ] End-to-end integration tests
- [ ] Documentation and demo