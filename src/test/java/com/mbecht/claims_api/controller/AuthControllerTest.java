package com.mbecht.claims_api.controller;

import com.mbecht.claims_api.dto.LoginResponse;
import com.mbecht.claims_api.exception.InvalidCredentialsException;
import com.mbecht.claims_api.service.AuthService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web slice test for AuthController. AuthService is mocked, so these tests only exercise HTTP
 * concerns: request binding, validation, status codes and response shape - not SecurityConfig,
 * which {@code addFilters = false} skips here (see the class comment on why: a plain
 * {@code @Configuration} class, which is all SecurityConfig is, isn't one of the stereotypes
 * {@code @WebMvcTest} scans, so without this it runs against Spring Boot's zero-config security
 * default instead of ours). SecurityConfig's actual behavior - permitAll on this path, a real
 * token coming back - is exercised for real against the full app in AuthIntegrationTest instead.
 */
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    void login_validRequest_returns200WithAccessToken() throws Exception {
        when(authService.login(any())).thenReturn(new LoginResponse("signed.jwt.token", "Bearer", 1800L));

        String validRequest = """
                {
                "username": "alice.holder",
                "password": "password123"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("signed.jwt.token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(1800));
    }

    @Test
    void login_invalidCredentials_returns401WithGenericMessage() throws Exception {
        when(authService.login(any())).thenThrow(new InvalidCredentialsException());

        String request = """
                {
                "username": "alice.holder",
                "password": "wrong-password"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.detail").value("The username and/or password are not correct."));
    }

    @Test
    void login_unknownUsername_returns401WithTheSameGenericMessage() throws Exception {
        // Same stub, same assertions as the wrong-password case above on purpose: from the
        // controller/AuthService boundary outward, the two failure modes are indistinguishable.
        when(authService.login(any())).thenThrow(new InvalidCredentialsException());

        String request = """
                {
                "username": "ghost.user",
                "password": "whatever"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.detail").value("The username and/or password are not correct."));
    }

    @Test
    void login_blankUsername_returns400WithoutCallingService() throws Exception {
        String request = """
                {
                "username": "",
                "password": "password123"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("username"));

        verifyNoInteractions(authService);
    }

    @Test
    void login_blankPassword_returns400WithoutCallingService() throws Exception {
        String request = """
                {
                "username": "alice.holder",
                "password": ""
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("password"));

        verifyNoInteractions(authService);
    }
}
