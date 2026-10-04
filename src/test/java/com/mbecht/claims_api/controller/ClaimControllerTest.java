package com.mbecht.claims_api.controller;

import com.mbecht.claims_api.dto.ClaimDetailResponse;
import com.mbecht.claims_api.dto.PageResponse;
import com.mbecht.claims_api.entity.ClaimStatus;
import com.mbecht.claims_api.exception.ErrorCode;
import com.mbecht.claims_api.exception.InvalidRequestException;
import com.mbecht.claims_api.exception.ResourceNotFoundException;
import com.mbecht.claims_api.service.ClaimFilter;
import com.mbecht.claims_api.service.ClaimService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
                ClaimDetailResponse response = new ClaimDetailResponse(
                        42L,
                        "CLM-000042",
                        LocalDateTime.parse("2026-01-15T10:00:00"),
                        new BigDecimal("500.00"),
                        "Test claim description",
                        ClaimStatus.SUBMITTED,
                        LocalDateTime.now(),
                        LocalDateTime.now(),
                        new ClaimDetailResponse.PolicyInfo(
                                123,
                                LocalDate.parse("2025-06-01"),
                                LocalDate.parse("2026-06-01"),
                                new BigDecimal("1000.00")));

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
        void getClaim_existingId_returns200WithClaimDetails() throws Exception {
                ClaimDetailResponse response = new ClaimDetailResponse(
                        42L,
                        "CLM-000042",
                        LocalDateTime.parse("2026-01-15T10:00:00"),
                        new BigDecimal("500.00"),
                        "Test claim description",
                        ClaimStatus.SUBMITTED,
                        LocalDateTime.parse("2026-01-16T09:00:00"),
                        LocalDateTime.parse("2026-01-16T09:00:00"),
                        new ClaimDetailResponse.PolicyInfo(
                                123,
                                LocalDate.parse("2025-06-01"),
                                LocalDate.parse("2026-06-01"),
                                new BigDecimal("1000.00")));

                when(claimService.getClaim(42L)).thenReturn(response);

                mockMvc.perform(get("/api/claims/42"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.id").value(42))
                        .andExpect(jsonPath("$.claimNumber").value("CLM-000042"))
                        .andExpect(jsonPath("$.policy.policyNumber").value(123));
        }

        @Test
        void getClaim_unknownId_returns404WithClaimNotFound() throws Exception {
                when(claimService.getClaim(999L)).thenThrow(
                        new ResourceNotFoundException(ErrorCode.CLAIM_NOT_FOUND, "No claim found with id 999"));

                mockMvc.perform(get("/api/claims/999"))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.errorCode").value("CLAIM_NOT_FOUND"));
        }

        @Test
        void getClaim_nonNumericId_returns400WithMalformedRequest() throws Exception {
                mockMvc.perform(get("/api/claims/abc"))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.errorCode").value("MALFORMED_REQUEST"))
                        .andExpect(jsonPath("$.errors[0].field").value("id"));

                verifyNoInteractions(claimService);
        }

        @Test
        void listClaims_noFilters_usesDefaultPagingAndNoStatus() throws Exception {
                when(claimService.listClaims(any(), any())).thenReturn(
                        new PageResponse<>(List.of(), 0, 20, 0, 0));

                mockMvc.perform(get("/api/claims"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.page").value(0))
                        .andExpect(jsonPath("$.size").value(20))
                        .andExpect(jsonPath("$.totalElements").value(0))
                        .andExpect(jsonPath("$.totalPages").value(0));

                verify(claimService).listClaims(new ClaimFilter(null, null, null, null),
                        PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "submittedAt")));
        }

        @Test
        void listClaims_sortParameter_isPassedToService() throws Exception {
                when(claimService.listClaims(any(), any())).thenReturn(
                        new PageResponse<>(List.of(), 0, 20, 0, 0));

                mockMvc.perform(get("/api/claims").param("sort", "amount,asc"))
                        .andExpect(status().isOk());

                verify(claimService).listClaims(new ClaimFilter(null, null, null, null),
                        PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "amount")));
        }

        @Test
        void listClaims_invalidSortField_returns400NamingTheFieldAsInvalid() throws Exception {
                when(claimService.listClaims(any(), any())).thenThrow(new InvalidRequestException("sort",
                        "Invalid sort field 'description'. Allowed sort fields: submittedAt, amount, incidentDate."));

                mockMvc.perform(get("/api/claims").param("sort", "description,asc"))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                        .andExpect(jsonPath("$.errors[0].field").value("sort"))
                        .andExpect(jsonPath("$.errors[0].message").value(
                                "Invalid sort field 'description'. Allowed sort fields: submittedAt, amount, incidentDate."));
        }

        @Test
        void listClaims_invalidSortDirection_returns400WithoutCallingService() throws Exception {
                mockMvc.perform(get("/api/claims").param("sort", "amount,up"))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                        .andExpect(jsonPath("$.errors[0].field").value("sort"))
                        .andExpect(jsonPath("$.errors[0].message").value(
                                "Invalid sort direction 'up'. Allowed directions: asc, desc."));

                verifyNoInteractions(claimService);
        }

        @Test
        void listClaims_multipleSortParameters_areAppliedInOrder() throws Exception {
                when(claimService.listClaims(any(), any())).thenReturn(
                        new PageResponse<>(List.of(), 0, 20, 0, 0));

                mockMvc.perform(get("/api/claims").param("sort", "amount,asc").param("sort", "incidentDate,desc"))
                        .andExpect(status().isOk());

                verify(claimService).listClaims(new ClaimFilter(null, null, null, null),
                        PageRequest.of(0, 20, Sort.by(Sort.Order.asc("amount"), Sort.Order.desc("incidentDate"))));
        }

        @Test
        void listClaims_pageAndSizeParameters_arePassedThrough() throws Exception {
                when(claimService.listClaims(any(), any())).thenReturn(
                        new PageResponse<>(List.of(), 2, 5, 0, 0));

                mockMvc.perform(get("/api/claims").param("page", "2").param("size", "5"))
                        .andExpect(status().isOk());

                verify(claimService).listClaims(new ClaimFilter(null, null, null, null),
                        PageRequest.of(2, 5, Sort.by(Sort.Direction.DESC, "submittedAt")));
        }

        @Test
        void listClaims_negativePage_returns400WithoutCallingService() throws Exception {
                mockMvc.perform(get("/api/claims").param("page", "-1"))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                        .andExpect(jsonPath("$.errors[0].field").value("page"))
                        .andExpect(jsonPath("$.errors[0].message").value("Invalid page -1. Page must be 0 or greater."));

                verifyNoInteractions(claimService);
        }

        @Test
        void listClaims_zeroSize_returns400WithoutCallingService() throws Exception {
                mockMvc.perform(get("/api/claims").param("size", "0"))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                        .andExpect(jsonPath("$.errors[0].field").value("size"))
                        .andExpect(jsonPath("$.errors[0].message").value("Invalid size 0. Size must be 1 or greater."));

                verifyNoInteractions(claimService);
        }

        @Test
        void listClaims_statusParameter_passesStatusToService() throws Exception {
                when(claimService.listClaims(any(), any())).thenReturn(
                        new PageResponse<>(List.of(), 0, 20, 0, 0));

                mockMvc.perform(get("/api/claims").param("status", "PAID"))
                        .andExpect(status().isOk());

                verify(claimService).listClaims(eq(new ClaimFilter(ClaimStatus.PAID, null, null, null)), any(Pageable.class));
        }

        @Test
        void listClaims_policyNumberAndInclusiveDateRange_passedToService() throws Exception {
                when(claimService.listClaims(any(), any())).thenReturn(
                        new PageResponse<>(List.of(), 0, 20, 0, 0));

                mockMvc.perform(get("/api/claims")
                                .param("policyNumber", "1001")
                                .param("submittedFrom", "2026-01-01")
                                .param("submittedTo", "2026-02-01"))
                        .andExpect(status().isOk());

                verify(claimService).listClaims(
                        eq(new ClaimFilter(null, 1001, LocalDate.parse("2026-01-01"), LocalDate.parse("2026-02-01"))),
                        any(Pageable.class));
        }

        @Test
        void listClaims_malformedDate_returns400WithMalformedRequest() throws Exception {
                mockMvc.perform(get("/api/claims").param("submittedFrom", "yesterday"))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.errorCode").value("MALFORMED_REQUEST"))
                        .andExpect(jsonPath("$.errors[0].field").value("submittedFrom"));

                verifyNoInteractions(claimService);
        }

        @Test
        void listClaims_unknownStatus_returns400WithMalformedRequest() throws Exception {
                mockMvc.perform(get("/api/claims").param("status", "FOO"))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.errorCode").value("MALFORMED_REQUEST"))
                        .andExpect(jsonPath("$.errors[0].field").value("status"));

                verifyNoInteractions(claimService);
        }

        @Test
        void listClaims_sizeAboveMaximum_isClampedToFifty() throws Exception {
                when(claimService.listClaims(any(), any())).thenReturn(
                        new PageResponse<>(List.of(), 0, 50, 0, 0));

                mockMvc.perform(get("/api/claims").param("size", "500"))
                        .andExpect(status().isOk());

                verify(claimService).listClaims(new ClaimFilter(null, null, null, null),
                        PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "submittedAt")));
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
