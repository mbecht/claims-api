# 0002. Use Flyway for migrations

**Date:** 2026-09-27
**Status:** Accepted

## Context
Separate paths were created to store database migrations and dev-data changes.
Dev seed data is kept separate from migrations so it will only load on the dev profile and has no risk of entering production environment.

## Decision
Flyway manages database schema changes with new versions for each change so that multiple users have access to an in-sync and validated schema.

## Alternatives considered
**Hibernate auto-DDL** - It generates schema changes at runtime by inspecting diff, but it does not store version records of what changed each time, and when changes were made.

## Consequences
- Every schema change requires a new SQL file with a new version number.