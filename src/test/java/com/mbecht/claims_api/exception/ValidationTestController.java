package com.mbecht.claims_api.exception;

import java.math.BigDecimal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test-only endpoint used to exercise GlobalExceptionHandler's validation-error
 * mapping, since no real request DTO exists yet.
 */
@RestController
class ValidationTestController {

    @PostMapping("/test/validate")
    ResponseEntity<Void> validate(@Valid @RequestBody TestRequest request) {
        return ResponseEntity.ok().build();
    }

    record TestRequest(
            @NotNull @Positive BigDecimal amount,
            @NotBlank String description) {
    }
}
