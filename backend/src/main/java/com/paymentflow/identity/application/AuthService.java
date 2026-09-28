package com.paymentflow.identity.application;

import com.paymentflow.identity.domain.Role;
import com.paymentflow.identity.domain.User;
import com.paymentflow.identity.infrastructure.JwtService;
import com.paymentflow.identity.infrastructure.UserRepository;
import com.paymentflow.shared.domain.BusinessRuleViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public record TokenResult(String token, String userId, String role) {
    }

    @Transactional
    public TokenResult register(String email, String rawPassword) {
        String normalizedEmail = email.toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new BusinessRuleViolationException("email already registered: " + email);
        }
        User user = new User(normalizedEmail, passwordEncoder.encode(rawPassword), Role.CUSTOMER);
        userRepository.save(user);
        String token = jwtService.issueToken(user.getId(), user.getEmail(), user.getRole().name());
        return new TokenResult(token, user.getId().toString(), user.getRole().name());
    }

    public TokenResult login(String email, String rawPassword) {
        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new BusinessRuleViolationException("invalid credentials"));
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new BusinessRuleViolationException("invalid credentials");
        }
        String token = jwtService.issueToken(user.getId(), user.getEmail(), user.getRole().name());
        return new TokenResult(token, user.getId().toString(), user.getRole().name());
    }
}
