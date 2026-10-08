package com.mbecht.claims_api;

import com.mbecht.claims_api.dto.LoginResponse;
import com.mbecht.claims_api.entity.Role;
import com.mbecht.claims_api.entity.User;
import com.mbecht.claims_api.repository.UserRepository;
import com.mbecht.claims_api.security.TokenService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises /api/auth/login and the SecurityFilterChain's JWT verification against the full
 * app: real Postgres (Testcontainers), real BCrypt check, a real signed token coming back, and
 * that token actually getting a protected endpoint past authentication. AuthControllerTest
 * covers AuthController's own HTTP concerns in isolation with AuthService mocked; this test is
 * what proves the pieces it mocks actually fit together.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthIntegrationTest {

    private static final String RAW_PASSWORD = "password123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @BeforeEach
    void seedUser() {
        userRepository.save(new User("test.login.user", passwordEncoder.encode(RAW_PASSWORD), Role.POLICYHOLDER));
    }

    @Test
    void login_validCredentials_returnsRealSignedToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequestJson("test.login.user", RAW_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(matchesPattern("^[\\w-]+\\.[\\w-]+\\.[\\w-]+$")))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(1800));
    }

    @Test
    void login_wrongPassword_returns401WithGenericMessage() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequestJson("test.login.user", "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.detail").value("The username and/or password are not correct."));
    }

    @Test
    void login_unknownUsername_returns401WithTheSameGenericMessage() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequestJson("no.such.user", "whatever")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.detail").value("The username and/or password are not correct."));
    }

    @Test
    void claimsEndpoint_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/claims/999999"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void claimsEndpoint_withRealTokenFromLogin_getsPastAuthentication() throws Exception {
        String loginResponseBody = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequestJson("test.login.user", RAW_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = objectMapper.readValue(loginResponseBody, LoginResponse.class).accessToken();

        // 404, not 401/403: the token got past authentication, and the claim simply doesn't exist.
        mockMvc.perform(get("/api/claims/999999").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CLAIM_NOT_FOUND"));
    }

    @Test
    void claimsEndpoint_withMalformedToken_returns401() throws Exception {
        mockMvc.perform(get("/api/claims/999999").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-real-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void claimsEndpoint_withExpiredToken_returns401() throws Exception {
        // Same secret as the app's real TokenService, so the signature verifies fine - only the
        // expiry (forced negative, so it expired the instant it was minted) should reject this.
        TokenService expiredTokenService = new TokenService(jwtSecret, -10);
        String expiredToken = expiredTokenService.generateToken("test.login.user", Role.POLICYHOLDER);

        mockMvc.perform(get("/api/claims/999999").header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("AUTHENTICATION_REQUIRED"));
    }

    private static String loginRequestJson(String username, String password) {
        return """
                {
                "username": "%s",
                "password": "%s"
                }
                """.formatted(username, password);
    }
}
