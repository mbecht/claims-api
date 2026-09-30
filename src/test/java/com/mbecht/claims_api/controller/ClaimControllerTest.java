package com.mbecht.claims_api.controller;

import com.mbecht.claims_api.dto.ClaimResponse;
import com.mbecht.claims_api.entity.ClaimStatus;
import com.mbecht.claims_api.service.ClaimService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web slice test for ClaimController. The service layer is mocked, so these
 * tests only exercise HTTP concerns: request binding, validation, status
 * codes and response shape - not the claim submission business logic
 * (covered separately by ClaimsServiceTest).
 */
@WebMvcTest(ClaimController.class)
class ClaimControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClaimService claimService;

        @Test
        void submitClaim_validRequest_returns201WithLocationAndSubmittedStatus() throws Exception {
                ClaimResponse response = new ClaimResponse(
                        42L,
                        "CLM-000042",
                        123,
                        LocalDateTime.parse("2026-01-15T10:00:00"),
                        new BigDecimal("500.00"),
                        "Test claim description",
                        ClaimStatus.SUBMITTED,
                        LocalDateTime.now());

                when(claimService.submitClaim(any())).thenReturn(response);

                String validRequest = """
                        {
                        "policyNumber": 123,
                        "incidentDate": "2026-01-15T10:00:00",
                        "amount": 500.00,
                        "description": "Test claim description"
                        }
                        """;

                mockMvc.perform(post("/api/claims")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validRequest))
                        .andExpect(status().isCreated())
                        .andExpect(header().string("Location", endsWith("/api/claims/42")))
                        .andExpect(jsonPath("$.id").value(42))
                        .andExpect(jsonPath("$.status").value("SUBMITTED"));
        }

        @Test
        void submitClaim_missingAmount_returns400WithValidationError() throws Exception {
                String requestMissingAmount = """
                        {
                        "policyNumber": 123,
                        "incidentDate": "2026-01-15T10:00:00",
                        "description": "Test claim description"
                        }
                        """;

                mockMvc.perform(post("/api/claims")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestMissingAmount))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                        .andExpect(jsonPath("$.errors", hasSize(1)))
                        .andExpect(jsonPath("$.errors[0].field").value("amount"))
                        .andExpect(jsonPath("$.errors[0].message").value("must not be null"));
        }

        @Test
        void submitClaim_negativeAmount_returns400WithValidationError() throws Exception {
                String requestNegativeAmount = """
                        {
                        "policyNumber": 123,
                        "incidentDate": "2026-01-15T10:00:00",
                        "amount": -100.00,
                        "description": "Test claim description"
                        }
                        """;

                mockMvc.perform(post("/api/claims")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestNegativeAmount))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                        .andExpect(jsonPath("$.errors", hasSize(1)))
                        .andExpect(jsonPath("$.errors[0].field").value("amount"))
                        .andExpect(jsonPath("$.errors[0].message").value("must be greater than 0"));
        }

        @Test 
        void submitClaim_futureIncidentDate_returns400WithValidationError() throws Exception {
            String requestFutureIncidentDate = """
                    {
                      "policyNumber": 123,
                      "incidentDate": "2030-01-15T10:00:00",
                      "amount": 500.00,
                      "description": "Test claim description"
                    }
                    """;

            mockMvc.perform(post("/api/claims")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestFutureIncidentDate))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.errors", hasSize(1)))
                    .andExpect(jsonPath("$.errors[0].field").value("incidentDate"))
                    .andExpect(jsonPath("$.errors[0].message").value("must be a date in the past or in the present"));
        }

        @Test
        void submitClaim_blankDescription_returns400WithValidationError() throws Exception {
            String requestBlankDescription = """
                    {
                      "policyNumber": 123,
                      "incidentDate": "2026-01-15T10:00:00",
                      "amount": 500.00,
                      "description": ""
                    }
                    """;

            mockMvc.perform(post("/api/claims")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBlankDescription))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.errors", hasSize(1)))
                    .andExpect(jsonPath("$.errors[0].field").value("description"))
                    .andExpect(jsonPath("$.errors[0].message").value("must not be blank"));
        }

        @Test
        void submitClaim_AmountWithMoreThanTwoDecimalPlaces_returns400WithValidationError() throws Exception {
            String requestAmountWithMoreThanTwoDecimalPlaces = """
                    {
                      "policyNumber": 123,
                      "incidentDate": "2026-01-15T10:00:00",
                      "amount": 500.123,
                      "description": "Test claim description"
                    }
                    """;

            mockMvc.perform(post("/api/claims")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestAmountWithMoreThanTwoDecimalPlaces))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.errors", hasSize(1)))
                    .andExpect(jsonPath("$.errors[0].field").value("amount"))
                    .andExpect(jsonPath("$.errors[0].message").value("numeric value out of bounds (<10 digits>.<2 digits> expected)"));
        }

        @Test
        void submitClaim_multipleInvalidFields_returns400WithValidationErrors() throws Exception {
            String requestMultipleInvalidFields = """
                    {
                      "policyNumber": 123,
                      "incidentDate": "2030-01-15T10:00:00",
                      "amount": -100.00,
                      "description": ""
                    }
                    """;

            mockMvc.perform(post("/api/claims")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestMultipleInvalidFields))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.errors", hasSize(3)))
                    .andExpect(jsonPath("$.errors[?(@.field == 'incidentDate')].message").value("must be a date in the past or in the present"))
                    .andExpect(jsonPath("$.errors[?(@.field == 'amount')].message").value("must be greater than 0"))
                    .andExpect(jsonPath("$.errors[?(@.field == 'description')].message").value("must not be blank"));
        }

        @Test
        void submitClaim_malformedJson_returns400WithValidationError() throws Exception {
            String malformedJsonRequest = """
                    {
                      "policyNumber": 123,
                      "incidentDate": "2026-01-15T10:00:00",
                      "amount": 500.00,
                      "description": "Test claim description"
                    """;

            mockMvc.perform(post("/api/claims")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(malformedJsonRequest))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("MALFORMED_REQUEST"))
                    .andExpect(jsonPath("$.errors", hasSize(1)))
                    .andExpect(jsonPath("$.errors[0].field").value("requestBody"))
                    .andExpect(jsonPath("$.errors[0].message").value("Malformed JSON request"));
        

        verifyNoInteractions(claimService);
        }
        
}
