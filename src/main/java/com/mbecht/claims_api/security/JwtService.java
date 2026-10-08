package com.mbecht.claims_api.security;

import com.mbecht.claims_api.entity.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

/**
 * Signs and verifies the HS256 JWTs described in CLAUDE.md's Authentication section:
 * subject = username, a role claim, 30-minute (configurable) expiry.
 * Registered as a {@code @Bean} in SecurityConfig rather than scanned as a {@code @Component}:
 * {@code @WebMvcTest} slices only scan a narrow set of stereotypes, but they do process whatever
 * a Security {@code @Configuration} class itself defines, so this keeps the bean visible there too.
 */
public class JwtService {

    private static final String ROLE_CLAIM = "role";

    private final SecretKey key;
    private final long expirationSeconds;

    public JwtService(String secret, long expirationSeconds) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.expirationSeconds = expirationSeconds;
    }

    public String generateToken(String username, Role role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(username)
                .claim(ROLE_CLAIM, role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirationSeconds)))
                .signWith(key)
                .compact();
    }

    /**
     * Verifies the signature and expiry, throwing {@link JwtException} for anything wrong with
     * the token (bad signature, malformed, expired). Callers treat any such failure as "not authenticated".
     */
    public Claims parseAndValidate(String token) {
        Jws<Claims> jws = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token);
        return jws.getPayload();
    }

    public String extractUsername(Claims claims) {
        return claims.getSubject();
    }

    public Role extractRole(Claims claims) {
        return Role.valueOf(claims.get(ROLE_CLAIM, String.class));
    }
}
