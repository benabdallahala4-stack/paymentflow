package com.paymentflow.identity.api;

import com.paymentflow.identity.application.AuthService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    public record RegisterRequest(@Email @NotBlank String email, @NotBlank @Size(min = 8) String password) {
    }

    public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {
    }

    public record TokenResponse(String accessToken, String userId, String role) {
    }

    @PostMapping("/register")
    public TokenResponse register(@RequestBody RegisterRequest request) {
        var result = authService.register(request.email(), request.password());
        return new TokenResponse(result.token(), result.userId(), result.role());
    }

    @PostMapping("/login")
    public TokenResponse login(@RequestBody LoginRequest request) {
        var result = authService.login(request.email(), request.password());
        return new TokenResponse(result.token(), result.userId(), result.role());
    }
}
