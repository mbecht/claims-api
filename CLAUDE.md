# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a portfolio project to demonstrate my (the user) understanding of the tech stack and working alongside AI. 
I must understand every change. Explain what you did and why after each task.

Work on one user story at a time. Don't add dependencies without asking.

Never commit or push. I do all Git operations myself.

Tech stack: Java 25, Spring Boot 4.1, Maven, PostgreSQL, JUnit5, Testcontainers.

## User Stories

Seventeen stories in six phases. Each phase builds on the one before, so finish and test one before starting the next. Tick acceptance criteria as they pass.
Phase 1 — Foundation
1. Runnable project skeleton. As a developer, I want a Spring Boot app and Postgres that start with one command, so every feature builds on a working base.
[ ] docker compose up starts the app and database
[ ] GET /actuator/health returns 200
[ ] GitHub Actions builds and runs tests on every push
2. Database schema and migrations. As a developer, I want the schema managed by Flyway, so database changes are versioned and repeatable.
[ ] Migrations create policies, claims and users tables
[ ] Seed data adds sample policies (active and expired) and users for each role
[ ] A Testcontainers test confirms migrations run on a clean Postgres
3. Consistent error responses. As an API consumer, I want every error in the same format, so failures are predictable to handle.
[ ] A global exception handler returns status, error code, message and timestamp
[ ] Validation errors list each invalid field
[ ] 400, 404 and 409 cases are covered by tests
Phase 2 — Core claims
4. Submit a claim. As a policyholder, I want to file a claim with my policy number, incident date, amount and description, so I can request reimbursement.
[ ] POST /api/claims creates a claim with status Submitted and returns 201
[ ] Missing or invalid fields (negative amount, future date) return 400
5. Validate coverage. As the insurer, I want claims checked against the policy, so we never accept a claim we don't cover.
[ ] Unknown policy → 404; expired policy → 422 with a clear message
[ ] Incident date outside the coverage period is rejected
[ ] Unit tests cover each rule, written before implementation
6. View and search claims. As a user, I want to see a claim's details and a filtered list of claims, so I can track their progress.
[ ] GET /api/claims/{id} returns full claim details
[ ] GET /api/claims supports filtering by status and paging
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

This is a Spring Boot project generated from Spring Initializr and is currently a bare skeleton — no controllers,
entities, repositories, or business logic have been added yet. The base package is `com.mbecht.claims_api`.

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

# Run the app locally against the dev Testcontainers Postgres instance
./mvnw.cmd test-run          # invokes TestClaimsApiApplication (see below)
```

## Architecture

- **Java 25**, Spring Boot 4.1.1 (`spring-boot-starter-parent`).
- Dependencies: `web` (Spring MVC), `data-jpa`, `validation`, `actuator`, PostgreSQL driver (runtime), plus
  Testcontainers-based equivalents for each starter in test scope.
- **Database**: PostgreSQL via Spring Data JPA. No datasource is configured in `application.properties` for local/dev
  runs — instead, `TestcontainersConfiguration` (in `src/test`) spins up a `postgres:latest` Testcontainers instance
  and wires it in automatically via `@ServiceConnection`. `TestClaimsApiApplication.main()` boots the full app with
  that Testcontainers config applied, which is the standard way to run the app locally during development (Spring
  Boot's Testcontainers-at-development-time support — see `HELP.md`). Production deployments will need a real
  datasource configured via `application.properties`/environment/profile once one exists.
- Main entry point: `ClaimsApiApplication` (`src/main/java/com/mbecht/claims_api/ClaimsApiApplication.java`).
- Test entry point for local dev runs: `TestClaimsApiApplication` (`src/test/java/com/mbecht/claims_api/TestClaimsApiApplication.java`).
