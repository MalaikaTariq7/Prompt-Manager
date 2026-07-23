package com.promptmanager.prompt_service.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.promptmanager.prompt_service.dto.LoginRequest;
import com.promptmanager.prompt_service.dto.LoginResponse;
import com.promptmanager.prompt_service.security.JwtService;

@Service
public class AuthService {

    private final JwtService jwtService;
    private final String authUsername;
    private final String authPassword;

    public AuthService(
            JwtService jwtService,
            @Value("${auth.username}") String authUsername,
            @Value("${auth.password}") String authPassword) {

        this.jwtService = jwtService;
        this.authUsername = authUsername;
        this.authPassword = authPassword;
    }

    public LoginResponse login(LoginRequest loginRequest) {

        boolean usernameMatches =
                authUsername.equals(loginRequest.getUsername());

        boolean passwordMatches =
                authPassword.equals(loginRequest.getPassword());

        if (!usernameMatches || !passwordMatches) {
            throw new IllegalArgumentException(
                    "Invalid username or password"
            );
        }

        String token = jwtService.generateToken(
                loginRequest.getUsername()
        );

        return new LoginResponse(
                token,
                "Bearer",
                jwtService.getExpirationMs()
        );
    }
}