# 0007. Stateless JWT authentication

**Date:** 2026-10-08
**Status:** Accepted

## Context
When a user attempts to login, the credentials they entered go to Spring's AuthenticationManager. We check that the user exists and the BCrypt hash matches the password they entered. On a success, a token is signed as a JWT with HMAC-SHA256 using the secret from JWT_SECRET

## Decision
The benefit of this token authorization is that it will be carreid for every subsequent request for as long as the token is active, rather than as long as the session is uninterrupted.

## Alternatives considered
A server-session model of a JSESSIONID cookie and in-memory storage can work, but would not survive a restart. JWT survives for as long as we allow the token to be active (30 minutes in this case).

## Consequences
JWT cannot be revoked early within the scope of this project. It is valid until it expires.