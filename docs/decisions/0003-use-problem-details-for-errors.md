# 0003. Use Problem Details for errors

**Date:** 2026-09-28
**Status:** Accepted

## Context
We are using RFC 9457 standard JSON shape for HTTP API error responses, as Spring Boot 4 has built-in support that fits the project needs.

## Decision
Problem Details is extensible, so it was simple to add errorCodes alongside standard HTTP status to describe the specific failure to the client.

## Alternatives considered
A custom JSON shape could have been designed to exactly fit the business needs and nothing more, however RFC 9457 already meets the requirements.

## Consequences
- Clients need to handle application/problem+json content type instead of application/json.