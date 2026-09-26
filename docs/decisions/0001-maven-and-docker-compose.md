# 0001. Use Maven and Docker Compose

**Date:** 2026-09-26
**Status:** Accepted

# Context
The project needed a build tool to manage dependencies and run tests. 
The app should be able to run locally with a PostgreSQL database from one command.

## Decision
Use Maven with the Maven wrapper as the build tool, Docker Compose to run the app and Postgres as two containers.

## Alternatives considered
**Installing Postgres directly on machine** - Works for a single user but creates issues for multiple users that each configure using different versions.

## Consequences
- Requires Docker Desktop to be installed and running
