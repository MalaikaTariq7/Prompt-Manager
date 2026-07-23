package com.promptmanager.prompt_service.dto;

public class LoginResponse {

    private String token;
    private String tokenType;
    private long expiresIn;

    // Default Constructor
    public LoginResponse() {
    }

    // Parameterized Constructor
    public LoginResponse(String token, String tokenType, long expiresIn) {
        this.token = token;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
    }

    // Getters & Setters

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }
}