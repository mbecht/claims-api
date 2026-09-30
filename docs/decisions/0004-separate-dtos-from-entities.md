# 0004. Separate DTOs from Entities

**Date:** 2026-09-30
**Status:** Accepted

## Context
A DTO carries data between client and server without business logic. The request knows what fields are required (ex: policyNumber) as well as the proper format the values need to be in (ex: a postive integer).

## Decision
Separating DTOs from entities improves security by obfuscating the internal data model and limiting what data is received from clients and sent back in response. For example, clients cannot override their role or bypass a status through a DTO that doesn't accept those fields.

## Alternatives considered
None.

## Consequences
- Implementation of DTOs need to be careful and precise to prevent sensitive data from exposure.