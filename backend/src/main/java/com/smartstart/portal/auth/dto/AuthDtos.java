package com.smartstart.portal.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank @Size(max = 50) String username,
            @NotBlank @Size(max = 200) String password
    ) {
    }

    public record LoginResponse(String token, Instant expiresAt, AdminUserResponse user) {
    }

    public record AdminUserResponse(Long id, String username, String fullName, String role) {
    }

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 8, max = 100, message = "must be between 8 and 100 characters") String newPassword
    ) {
    }
}
