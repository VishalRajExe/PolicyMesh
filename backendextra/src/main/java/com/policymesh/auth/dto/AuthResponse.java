package com.policymesh.auth.dto;

public record AuthResponse(
        String token,
        String tokenType,
        String email,
        String role,
        long expiresInMs
) {
    public static AuthResponse of(String token, String email, String role, long expiresInMs) {
        return new AuthResponse(token, "Bearer", email, role, expiresInMs);
    }
}
