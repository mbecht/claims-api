# 0005. Coverage rules in domain validator

**Date:** 2026-10-02
**Status:** Acccepted

## Context
The business rules live in a dependency-free class called CoverageValidator. They compare fields on the claim (incident date and claim amount) against values on the policy to determine if the claim is eligible for submission.

## Decision
The coverage validator class is currently lightweight and has no dependencies. It can easily be called from the claim service and CoverageValidatorTest suite.

## Alternatives considered
The claims service already handles a ResourceNotFoundException in the event a claim uses a non-existent policy number. Additional business rules can exist  in this same method because the claim and policy objects are both accessible.

## Consequences
As more user stories are implemented, the business logic will grow in size and become more complex. In an effort to make this process easy to read and navigate, we decided early on to separate coverage validation from the claim service submission method.