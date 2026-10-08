package com.mbecht.claims_api.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}
