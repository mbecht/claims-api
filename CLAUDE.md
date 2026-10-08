# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This project is a Spring Boot REST API that takes an insurance claim from submission to payout, enforcing business rules, role-based permissions and a full audit trail.
The user case example is that an insurer needs a backend service where policyholders file claims, adjusters review them, and supervisors handle high-value or suspicious cases. Every change must be traceable for auditors.
This is an educational project so the claims, users, and data are all fictional.

This is a portfolio project to demonstrate my (the user) understanding of the tech stack and working alongside AI. 
I must understand every change. Explain what you did and why after each task.

Work on one user story at a time. Don't add dependencies without asking.

Never commit or push. I do all Git operations myself.

Tech stack: Java 25, Spring Boot 4.1, Maven, PostgreSQL, JUnit5, Testcontainers.

## Business Rules

We need to establish and maintain business rules and coverage criteria for this fictional insurance company.
Every policy requires a policy number. Every policy has a one year duration.
An incident or claim must occur on or after the coverage start date.
An incident or claim must occur on or before the coverage end date (compare by truncating the incident timestamp to a date first).
An expired policy can still accept a claim so long as the recorded incident date occurred while the policy was still active — expiration alone is not a rejection reason; only the incident-date-vs-coverage-period check is.
The claim amount must not exceed the coverage limit amount; an amount equal to the limit is valid.

## Backlog (business rules out of scope for story 5)

- Cumulative/aggregate claim exposure — nothing stops multiple claims against one policy from each individually passing the coverage-limit check but together exceeding it. Story 5 checks each claim's amount against the limit in isolation; aggregate tracking is a different rule, likely tied to the fraud-flagging work in story 13.
- Policy cancellation/lapse as a distinct state — "expired" currently only means past `coverage_end` (date-driven). There's no way to represent a policy cancelled mid-term (non-payment, fraud, policyholder request). Would need a `status` column and its own rule set if ever wanted.
- Policy renewal semantics — whether a renewed policy reuses its `policy_number` or gets a new one (the schema currently treats `policy_number` as unique forever). Not urgent until a create/renew-policy endpoint exists.
- Validation error precedence — which error should win when a request violates more than one rule at once. Not a concern yet with only two independent checks in story 5, but worth a one-line convention once more rules stack up.

## Authentication
1. Client send POST to /api/auth/login with {username, password}
- Public endpoints are POST api/auth/login, GET /actuator/health
2. Server checks password against the BCrypt hash in the users table
3. Server sends 200 with {accessToken, tokenType: "Bearer", expiresIn: 1800}
- Token type is JWT signed by the server.
- Lifetime is 30 minutes.
4. Client sends GET to api/claims with header Authorization: Bearer <token>
- Token contains username (sub), role, issued-at, expiry.
- Contents of token are readable by anyone who has the token.
5. Server verifies the token's signature and expiry to allow the request.
- Note that refresh tokens and logout are out of scope.

## User Stories

Seventeen stories in six phases. Each phase builds on the one before, so finish and test one before starting the next. Tick acceptance criteria as they pass.
Phase 1 — Foundation
1. Runnable project skeleton. As a developer, I want a Spring Boot app and Postgres that start with one command, so every feature builds on a working base.
[x] docker compose up starts the app and database
[x] GET /actuator/health returns 200
[x] GitHub Actions builds and runs tests on every push
2. Database schema and migrations. As a developer, I want the schema managed by Flyway, so database changes are versioned and repeatable.
[x] Migrations create policies, claims and users tables
[x] Seed data adds sample policies (active and expired) and users for each role
[x] A Testcontainers test confirms migrations run on a clean Postgres
3. Consistent error responses. As an API consumer, I want every error in the same format, so failures are predictable to handle.
[x] A global exception handler returns status, error code, message and timestamp
[x] Validation errors list each invalid field
[x] 400, 404 and 409 cases are covered by tests
Phase 2 — Core claims
4. Submit a claim. As a policyholder, I want to file a claim with my policy number, incident date, amount and description, so I can request reimbursement.
[x] POST /api/claims creates a claim with status Submitted and returns 201
[x] Missing or invalid fields (negative amount, future date) return 400
5. Validate coverage. As the insurer, I want claims checked against the policy, so we never accept a claim we don't cover.
[x] Unknown policy → 404
[x] Incident date outside the coverage period (coverage start through coverage end, inclusive; compare by truncating the incident timestamp to a date) → 422 — an expired policy is not itself a rejection reason
[x] Claim amount exceeding the policy's coverage limit → 422; an amount equal to the limit is valid
[x] Unit tests cover each rule, written before implementation
6. View and search claims. As a user, I want to see a claim's details and a filtered list of claims, so I can track their progress.
[x] GET /api/claims/{id} returns full claim details
[x] GET /api/claims supports filtering by status and paging
Phase 3 — Security
7. Log in with a token. As a user, I want to log in and receive a JWT, so the API knows who I am on each request.
[ ] POST /api/auth/login returns a token for valid credentials, 401 otherwise
[ ] Passwords are stored hashed (BCrypt)
[ ] All claim endpoints reject requests without a valid token
8. Role-based access. As the insurer, I want each role limited to its own actions, so data stays private.
[ ] Policyholders see and submit only their own claims; another person's claim returns 403 or 404
[ ] Adjusters and supervisors can see all claims
[ ] Tests prove each role's allowed and blocked actions
Phase 4 — Claim workflow
9. Enforce the claim lifecycle. As the insurer, I want claims to follow Submitted → Under Review → Approved / Denied → Paid, so no step is skipped.
[ ] Status rules live in the domain model, not the controller
[ ] Illegal transitions (e.g. Submitted → Paid) return 409
[ ] Unit tests cover every legal and illegal transition
10. Review and decide. As an adjuster, I want to pick up a claim, then approve or deny it with a reason, so decisions are documented.
[ ] Adjuster moves a claim to Under Review, then Approved or Denied
[ ] A denial requires a reason
11. Escalate large claims. As a supervisor, I want claims over $10,000 routed to me, so high-value payouts get extra review.
[ ] An adjuster approving a claim over $10,000 is refused with a clear message
[ ] A supervisor can approve any amount
[ ] The $10,000 limit is configurable, not hard-coded
12. Record payment. As a supervisor, I want to mark approved claims as paid, so the claim is closed.
[ ] Only Approved claims can become Paid
[ ] Status change and payment date save in one transaction
Phase 5 — Fraud rules and audit
13. Flag suspicious claims. As an adjuster, I want risky claims flagged automatically, so I review them more carefully.
[ ] Claims filed within 30 days of policy start are flagged
[ ] A third claim on the same policy within 12 months is flagged
[ ] Each flag stores its reason; new rules can be added without editing existing ones
14. Audit trail. As an auditor, I want a history of every change to a claim, so decisions can be traced.
[ ] Every status change records who, when, old value and new value
[ ] GET /api/claims/{id}/history returns the timeline, oldest first
[ ] Audit records are append-only (no update or delete endpoints)
Phase 6 — Polish and presentation
15. Interactive API docs. As a reviewer, I want Swagger UI, so I can explore and try the API without reading code.
[ ] Swagger UI is available at /swagger-ui.html with every endpoint described
[ ] Examples show how to log in and use the token
16. End-to-end integration tests. As a developer, I want tests that run full scenarios against a real database, so I trust the system as a whole.
[ ] A Testcontainers test walks one claim from submission to payment across all three roles
[ ] CI runs the full suite and blocks merging on failures
17. Documentation and demo. As a hiring manager, I want to understand and run the project in minutes, so I can judge the work quickly.
[ ] README written by me: purpose, architecture diagram, one-command setup, sample requests, next steps
[ ] 3–5 decision records in docs/decisions/
[ ] A short GIF or screenshots of the API in use

Definition of done
The project is ready to showcase when all of these are true:
[ ] All 17 stories' acceptance criteria are ticked
[ ] docker compose up works on a fresh machine using only the README
[ ] CI is green on the main branch
[ ] I can explain every package, class and test without looking anything up
[ ] README, decision records and demo GIF are pinned on my GitHub profile

## Project state

Stories 1–6 are complete: project skeleton/Docker/CI, Flyway-managed schema with dev-only seed data, a global
RFC 9457 exception handler, the claim submission endpoint, coverage validation against the policy, and claim
viewing and search (detail by ID, plus a paged, filtered and sorted list). The base package is
`com.mbecht.claims_api`, organized as:

- `controller` — `ClaimController` (`POST /api/claims`, `GET /api/claims/{id}`, `GET /api/claims`)
- `dto` — `SubmitClaimRequest`, `ClaimDetailResponse` (with nested `PolicyInfo`), `ClaimSummaryResponse`, `PageResponse`
- `entity` — `Claim`, `Policy`, `User`, `Role`, `ClaimStatus`
- `repository` — `ClaimRepository`, `PolicyRepository`, `ClaimSpecifications`
- `service` — `ClaimService`, `ClaimFilter`, `ClaimSortField`, `ClaimNumberGenerator`, `CoverageValidator`
- `exception` — `GlobalExceptionHandler`, `ErrorCode`, `ResourceNotFoundException`, `InvalidRequestException`, `ConflictException`, `BusinessRuleException`

`ClaimService.submitClaim` looks up the policy (404 via `ResourceNotFoundException` if unknown), then delegates to
`CoverageValidator.validate` to check the incident date against the coverage period and the claim amount against
the coverage limit, each violation raising a `BusinessRuleException` (422) with its own `ErrorCode`
(`POLICY_EXPIRED`, `POLICY_NOT_YET_ACTIVE`, `CLAIM_AMOUNT_EXCEEDS_POLICY_LIMIT`). `CoverageValidatorTest` covers the
three rules and their inclusive boundaries in isolation; `ClaimsServiceTest` covers the service's own orchestration
(policy lookup, claim number generation, persistence).

Reads: `ClaimService.getClaim` returns one claim or throws `CLAIM_NOT_FOUND` (404). `ClaimService.listClaims` takes a
`ClaimFilter` and a `Pageable`, builds one `Specification` from `ClaimSpecifications.matching` (each predicate returns
null when its filter is unset, so any combination works), and returns a `PageResponse`. The controller reads
`page` and `size` as explicit parameters, caps `size` at 50, and parses `sort` from the raw parameter map so that
repeated values work. The service maps the public sort names (`submittedAt`, `amount`, `incidentDate`) to entity
properties through `ClaimSortField`, rejects anything else with `InvalidRequestException` (400 `VALIDATION_FAILED`),
and appends `id` as a tiebreaker. `ClaimRepository` overrides the specification search with an `@EntityGraph` on
`policy` to avoid one query per claim. The `submittedFrom`/`submittedTo` filters compare against `createdAt`,
which the API calls `submittedAt`.

Sample request payloads for manual testing live in `requests/` at the repo root (not under `src/`, since they're
dev tooling, not part of the build).

## Commands

This project uses the Maven wrapper (`mvnw`/`mvnw.cmd`) — no need for a locally installed Maven.

```
# Build (compile + test)
./mvnw.cmd verify          # PowerShell/cmd
./mvnw verify               # Bash

# Run the app
./mvnw.cmd spring-boot:run

# Run all tests
./mvnw.cmd test

# Run a single test class
./mvnw.cmd test -Dtest=ClaimsApiApplicationTests

# Run a single test method
./mvnw.cmd test -Dtest=ClaimsApiApplicationTests#contextLoads

# Run the app locally against an ephemeral Testcontainers Postgres instance
# (separate container/database from docker compose — see Architecture below).
# Requires Docker Desktop running. Set the dev profile first to load seed data:
$env:SPRING_PROFILES_ACTIVE = "dev"    # PowerShell
./mvnw.cmd spring-boot:test-run        # invokes TestClaimsApiApplication (see below)
```

## Architecture

- **Java 25**, Spring Boot 4.1.1 (`spring-boot-starter-parent`).
- Dependencies: `webmvc` (Spring MVC), `data-jpa`, `flyway` + `flyway-database-postgresql`, `validation`, `actuator`,
  PostgreSQL driver (runtime), plus Testcontainers-based equivalents for each starter in test scope.
- **Database**: PostgreSQL via Spring Data JPA, schema managed by Flyway (ADR 0002). Migrations live in
  `src/main/resources/db/migration`; dev-only seed data lives in a separate `db/dev-data` location that's only
  added to Flyway's search path when the `dev` Spring profile is active (`application-dev.yml`), so it never runs
  in tests or production.
- **Two separate ways to run against Postgres locally — they use different, non-interchangeable databases:**
  - `docker compose up --build` — the app plus a **persistent** Postgres (`docker-compose.yml`, named volume,
    database `claims`, credentials from `.env`) as two containers (ADR 0001). The production-like path.
  - `./mvnw.cmd spring-boot:test-run` — boots `TestClaimsApiApplication`, which applies `TestcontainersConfiguration`
    (`src/test`) to spin up a **fresh, ephemeral** `postgres:latest` container every run via `@ServiceConnection`
    (database/user/password default to `test`; data doesn't survive past the run). This is Spring Boot's
    Testcontainers-at-development-time support (see `HELP.md`) — fast local iteration, not a substitute for the
    docker-compose path. Production deployments will need a real datasource configured via environment/profile.
- **Error handling**: a single `@RestControllerAdvice` (`GlobalExceptionHandler`) translates exceptions into
  RFC 9457 Problem Details responses with an added `errorCode` and `timestamp` extension property (ADR 0003).
- **Request/response shape**: controllers accept and return DTOs (`dto` package), never JPA entities directly, so
  the wire format stays decoupled from the persistence model (ADR 0004).
- Main entry point: `ClaimsApiApplication` (`src/main/java/com/mbecht/claims_api/ClaimsApiApplication.java`).
- Test entry point for local dev runs: `TestClaimsApiApplication` (`src/test/java/com/mbecht/claims_api/TestClaimsApiApplication.java`).
