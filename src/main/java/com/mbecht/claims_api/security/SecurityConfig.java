package com.mbecht.claims_api.security;

import com.mbecht.claims_api.exception.ErrorCode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.time.Instant;

/**
 * Wires JWT-based authentication into Spring Security (ADR-pending for story 7):
 * stateless sessions, CSRF disabled (no cookies are involved, so there's nothing for CSRF
 * to protect), /api/auth/login and /actuator/health public, everything else requires a
 * verified bearer token. Unauthenticated/forbidden responses are shaped the same way as
 * every other error in this API (see GlobalExceptionHandler) because Spring Security rejects
 * requests before they ever reach a controller, so @RestControllerAdvice can't touch them.
 * Also exposes the {@link PasswordEncoder} and {@link AuthenticationManager} beans the login
 * endpoint authenticates against (AuthController -&gt; AuthService -&gt; AuthenticationManager -&gt;
 * UserDetailsServiceImpl, which reads {@code users} via {@code UserRepository}).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    TokenService tokenService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-seconds}") long expirationSeconds) {
        return new TokenService(secret, expirationSeconds);
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, TokenService tokenService, ObjectMapper objectMapper)
            throws Exception {

        JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(tokenService);

        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login", "/actuator/health")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authEx) -> writeProblemDetail(
                                response,
                                objectMapper,
                                HttpStatus.UNAUTHORIZED,
                                ErrorCode.AUTHENTICATION_REQUIRED,
                                "A valid bearer token is required."))
                        .accessDeniedHandler((request, response, accessEx) -> writeProblemDetail(
                                response,
                                objectMapper,
                                HttpStatus.FORBIDDEN,
                                ErrorCode.ACCESS_DENIED,
                                "You do not have permission to perform this action.")))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private static void writeProblemDetail(
            jakarta.servlet.http.HttpServletResponse response,
            ObjectMapper objectMapper,
            HttpStatus status,
            ErrorCode errorCode,
            String detail)
            throws IOException {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setProperty("errorCode", errorCode);
        problem.setProperty("timestamp", Instant.now());

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), problem);
    }
}
