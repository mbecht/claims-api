package com.mbecht.claims_api.exception;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises every response shape GlobalExceptionHandler produces, through
 * ValidationTestController and ExceptionTestController, since no real
 * controller triggers these exceptions yet.
 */
@WebMvcTest(controllers = {ValidationTestController.class, ExceptionTestController.class})
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void validationFailureReturns400WithEveryInvalidField() throws Exception {
        String requestWithTwoInvalidFields = """
                {
                  "amount": -5,
                  "description": ""
                }
                """;

        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestWithTwoInvalidFields))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors", hasSize(2)))
                .andExpect(jsonPath("$.errors[0].field").value("amount"))
                .andExpect(jsonPath("$.errors[0].message").value("must be greater than 0"))
                .andExpect(jsonPath("$.errors[1].field").value("description"))
                .andExpect(jsonPath("$.errors[1].message").value("must not be blank"));
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        String brokenJson = "{ \"amount\": -5, ";

        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(brokenJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MALFORMED_REQUEST"))
                .andExpect(jsonPath("$.detail").value("The request body could not be read."));
    }

    @Test
    void unknownUrlReturns404() throws Exception {
        mockMvc.perform(get("/test/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NO_SUCH_ENDPOINT"));
    }

    @Test
    void resourceNotFoundReturns404WithItsOwnErrorCode() throws Exception {
        mockMvc.perform(get("/test/resource-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CLAIM_NOT_FOUND"))
                .andExpect(jsonPath("$.detail").value("Claim 999 not found."))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void conflictReturns409() throws Exception {
        mockMvc.perform(get("/test/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ILLEGAL_STATUS_TRANSITION"))
                .andExpect(jsonPath("$.detail").value("Cannot transition claim from SUBMITTED to PAID."));
    }

    @Test
    void businessRuleViolationReturns422() throws Exception {
        mockMvc.perform(get("/test/business-rule"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("BUSINESS_RULE_VIOLATION"))
                .andExpect(jsonPath("$.detail").value("Policy POL-123 is expired."));
    }

    @Test
    void unexpectedExceptionReturns500WithoutLeakingInternalDetails() throws Exception {
        mockMvc.perform(get("/test/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred."))
                .andExpect(content().string(not(containsString("hunter2"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))));
    }
}
