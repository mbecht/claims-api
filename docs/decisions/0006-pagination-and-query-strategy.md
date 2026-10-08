# 0006. Pagination and query strategy

**Date:** 2026-10-04
**Status:** Accepted

## Context
Our search claims GET request comes with a custom page formatting. Page size defaults to 20 records and is clamped at 50 records, so any request over 50 will still return a maxmimum of 50 records. The sort fields are also restricted to submittedAt (mapped to createdAt), amount, and incidentDate. Each can be sorted by descending or ascending value.

## Decision
The README documents exactly the fields and format that is useful for the scope of this project.

## Alternatives considered
Returning SpringData's Page directly, which includes fields like pageable, soprt, first, last, empty, and numberOfElements.

## Consequences
A framework upgrade can add or remove fields, and for generated clients that would mean failure at runtime.