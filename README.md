# Insurance Claims Processing API

[![CI](https://github.com/mbecht/claims-api/actions/workflows/ci.yml/badge.svg)](https://github.com/mbecht/claims-api/actions/workflows/ci.yml)

This personal project is a demonstration of a Spring Boot REST API that could be used for enterprise insurance software needs.
The backend service takes a mock insurance claim from submission to payout, enforcing business rules, role-based permissions and a full audit trail.

> **Status:** In progress - Story 1 of 17 complete (project skeleton, Docker, CI)

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
```
2. Copy the environment template and set a password
```
3. Start the app and database
```bash
   docker compose up --build
```
4. Check that it's running
```bash
   curl http://localhost:8080/actuator/health
   # {"status":"UP"}
```

## Running tests
```bash
./mvnw verify
```
Docker must be running; tests start a temporary Postgres container.

## Architecture
The app will run in one container with Postgre in another, connected by Docker Compose.
**TODO:** A diagram will be added as the project grows.

## Design decisions
See [docs/decisions](docs/decisions/) for the reasoning behind key choices.

## Roadmap
- [x] Project skeleton, Docker, CI
- [ ] Database schema and migrations
- [ ] Claim submission and validation
- [ ] Authentication and role-based access
- [ ] Claim workflow and approval limits
- [ ] Fraud rules and audit trail