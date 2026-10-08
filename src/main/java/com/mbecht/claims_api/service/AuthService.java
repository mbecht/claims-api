package com.mbecht.claims_api.service;

import com.mbecht.claims_api.dto.LoginRequest;
import com.mbecht.claims_api.dto.LoginResponse;
import com.mbecht.claims_api.entity.User;
import com.mbecht.claims_api.exception.InvalidCredentialsException;
import com.mbecht.claims_api.repository.UserRepository;
import com.mbecht.claims_api.security.TokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final long expiresInSeconds;

    public AuthService(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            TokenService tokenService,
            @Value("${app.jwt.expiration-seconds}") long expiresInSeconds) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.expiresInSeconds = expiresInSeconds;
    }

    public LoginResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        } catch (AuthenticationException ex) {
            // Same exception, same message, whether the username doesn't exist or the password
            // is wrong — see UserDetailsServiceImpl and InvalidCredentialsException for why.
            throw new InvalidCredentialsException();
        }

        // authenticate() above threw unless this user exists, so this lookup can't fail.
        User user = userRepository.findByUsername(request.username()).orElseThrow();

        String token = tokenService.generateToken(user.getUsername(), user.getRole());
        return new LoginResponse(token, "Bearer", expiresInSeconds);
    }
}
