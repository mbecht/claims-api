package com.mbecht.claims_api.service;

import com.mbecht.claims_api.dto.LoginRequest;
import com.mbecht.claims_api.dto.LoginResponse;
import com.mbecht.claims_api.entity.Role;
import com.mbecht.claims_api.entity.User;
import com.mbecht.claims_api.exception.InvalidCredentialsException;
import com.mbecht.claims_api.repository.UserRepository;
import com.mbecht.claims_api.security.TokenService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Covers AuthService's own logic: delegating to AuthenticationManager, issuing a token on
 * success, and - the point of story 7's deterrence requirement - throwing the exact same
 * exception whether AuthenticationManager rejected an unknown username or a wrong password.
 * Spring Security's DaoAuthenticationProvider is what actually folds both cases into one
 * BadCredentialsException upstream of this class (see UserDetailsServiceImpl); these tests
 * only prove AuthService doesn't undo that by re-exposing any distinguishing detail.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final long EXPIRES_IN_SECONDS = 1800L;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenService tokenService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(authenticationManager, userRepository, tokenService, EXPIRES_IN_SECONDS);
    }

    @Test
    void login_validCredentials_returnsTokenFromTokenService() {
        User user = new User("alice.holder", "hashed-password", Role.POLICYHOLDER);
        when(userRepository.findByUsername("alice.holder")).thenReturn(Optional.of(user));
        when(tokenService.generateToken("alice.holder", Role.POLICYHOLDER)).thenReturn("signed.jwt.token");

        LoginResponse response = authService.login(new LoginRequest("alice.holder", "password123"));

        assertEquals("signed.jwt.token", response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(EXPIRES_IN_SECONDS, response.expiresIn());

        ArgumentCaptor<UsernamePasswordAuthenticationToken> captor =
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(captor.capture());
        assertEquals("alice.holder", captor.getValue().getName());
        assertEquals("password123", captor.getValue().getCredentials());
    }

    @Test
    void login_unknownUsername_throwsInvalidCredentialsException() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(InvalidCredentialsException.class,
                () -> authService.login(new LoginRequest("ghost.user", "whatever")));

        verifyNoInteractions(userRepository);
        verifyNoInteractions(tokenService);
    }

    @Test
    void login_wrongPassword_throwsTheSameExceptionAsUnknownUsername() {
        // A different message than the unknown-username test, on purpose: AuthService must not
        // let anything about *why* AuthenticationManager failed leak into its own exception.
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Password does not match stored value"));

        InvalidCredentialsException ex = assertThrows(InvalidCredentialsException.class,
                () -> authService.login(new LoginRequest("alice.holder", "wrong-password")));

        assertNull(ex.getMessage());
        verifyNoInteractions(userRepository);
        verifyNoInteractions(tokenService);
    }
}
