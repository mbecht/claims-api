# Insurance Claims Processing API

[![CI](https://github.com/mbecht/claims-api/actions/workflows/ci.yml/badge.svg)](https://github.com/mbecht/claims-api/actions/workflows/ci.yml)

This personal project is a demonstration of a Spring Boot REST API that could be used for enterprise insurance software needs.
The backend service takes a mock insurance claim from submission to payout, enforcing business rules, role-based permissions and a full audit trail.

> **Status:** In progress - Story 3 of 17 complete

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
| 'alice' | Policyholder | Submitting and viewing her own claims |
| 'bob' | Policyholder | Access control test |
| 'carol' | Adjuster | Reviewing claims up to $10,000 |
| 'dave' | Supervisor | Approving claims over $10,000 |

Seed data lives in 'src/main/resources/db/dev-data/' and runs only when the 'dev' profile is active.

## Running tests
```bash
./mvnw verify
```
Docker must be running; tests start a temporary Postgres container.

## Error responses

All errors return a consistent JSON body following the [RFC 9457 Problem Details](https://www.rfc-editor.org/rfc/rfc9457) standard, with the content type of 'application/problem+json'. Every response includes a 'errorCode' as well as a UTC 'timestamp'.

| Error code | HTTP status | Meaning | Example cause |
|---|:---:|---|---|
| `VALIDATION_FAILED` | 400 | One or more fields are invalid; see `errors` | Negative claim amount |
| `MALFORMED_REQUEST` | 400 | The request body isn't valid JSON or has the wrong types | Text sent where a number is expected |
| `RESOURCE_NOT_FOUND` | 404 | The requested item or URL doesn't exist | Unknown claim ID |
| `CONFLICT` | 409 | The request conflicts with the item's current state | Moving a claim from Submitted straight to Paid |
| `BUSINESS_RULE_VIOLATION` | 422 | The request is valid but breaks a business rule | Filing a claim on an expired policy |
| `INTERNAL_ERROR` | 500 | Unexpected server error | Database unavailable |

> [!NOTE]
> For security, `500` responses return only a generic message. Details such as
> stack traces, class names, and SQL are logged on the server and never sent to
> the client. Validation errors also omit the rejected values, since they may
> contain personal data.

## Architecture
The app will run in one container with Postgre in another, connected by Docker Compose.

## Design decisions
See [docs/decisions](docs/decisions/) for the reasoning behind key choices.

## Roadmap
- [x] Project skeleton, Docker, CI
- [x] Database schema and migrations
- [ ] Claim submission and validation
- [ ] Authentication and role-based access
- [ ] Claim workflow and approval limits
- [ ] Fraud rules and audit trail