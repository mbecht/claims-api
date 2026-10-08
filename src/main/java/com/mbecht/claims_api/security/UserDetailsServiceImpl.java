package com.mbecht.claims_api.security;

import com.mbecht.claims_api.entity.User;
import com.mbecht.claims_api.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Loads a {@link User} for Spring Security's {@link org.springframework.security.authentication.AuthenticationManager}
 * to check the password against. Throwing {@link UsernameNotFoundException} here is what lets
 * {@code DaoAuthenticationProvider} fold "no such user" and "wrong password" into the same
 * {@code BadCredentialsException} (its {@code hideUserNotFoundExceptions} flag defaults to true) —
 * that's the actual mechanism behind AuthService returning one generic message for both cases.
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("No user with username " + username));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPasswordHash())
                .authorities("ROLE_" + user.getRole().name())
                .build();
    }
}
