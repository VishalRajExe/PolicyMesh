package com.policymesh.auth.dto;

import com.policymesh.auth.entity.Role;
import com.policymesh.auth.entity.User;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String name,
        String email,
        Role role,
        boolean enabled,
        Instant createdAt
) {
    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getRole(), u.isEnabled(), u.getCreatedAt());
    }
}
