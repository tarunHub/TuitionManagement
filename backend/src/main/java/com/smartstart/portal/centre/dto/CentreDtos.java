package com.smartstart.portal.centre.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class CentreDtos {

    private CentreDtos() {
    }

    public record CentreRequest(
            @NotBlank @Size(max = 150) String name,
            @NotBlank @Pattern(regexp = "[A-Za-z]{1,10}", message = "must be 1-10 letters") String studentCodePrefix,
            @Email @Size(max = 150) String contactEmail,
            @Size(max = 40) String contactPhone,
            @Size(max = 300) String address,
            Boolean active
    ) {
    }

    /** New centre plus, optionally, its first staff login. */
    public record CreateCentreRequest(@Valid CentreRequest centre, @Valid UserRequest firstUser) {
    }

    public record CentreResponse(Long id, String name, String studentCodePrefix, String contactEmail, String contactPhone,
                                 String address, boolean active, long userCount, long studentCount,
                                 boolean emailConfigured, boolean smsConfigured, Instant createdAt) {
    }

    public record UserRequest(
            @NotBlank @Size(min = 3, max = 50) @Pattern(regexp = "[A-Za-z0-9._@-]+", message = "letters, digits and . _ @ - only") String username,
            @Size(max = 100) String fullName,
            @NotBlank @Size(min = 8, max = 100, message = "must be between 8 and 100 characters") String password
    ) {
    }

    public record UpdateUserRequest(@Size(max = 100) String fullName, Boolean enabled) {
    }

    public record ResetPasswordRequest(
            @NotBlank @Size(min = 8, max = 100, message = "must be between 8 and 100 characters") String newPassword) {
    }

    public record UserResponse(Long id, String username, String fullName, String role, boolean enabled,
                               Long centreId, Instant createdAt) {
    }

    /** Email and SMS settings of the current centre. Passwords are never sent back, only whether one is saved. */
    public record MessagingSettingsResponse(
            String centreName, String studentCodePrefix,
            String mailFrom, String mailFromName, String smtpHost, Integer smtpPort, String smtpUsername,
            boolean smtpPasswordSet, boolean smtpAuth, boolean smtpStarttls, boolean emailConfigured,
            boolean smsEnabled, String smsGatewayUrl, String smsUsername, boolean smsPasswordSet, String smsCountryCode,
            boolean smsConfigured
    ) {
    }

    /** A blank password keeps the saved one; set clear…Password to remove it. */
    public record MessagingSettingsRequest(
            @Email @Size(max = 150) String mailFrom,
            @Size(max = 150) String mailFromName,
            @Size(max = 150) String smtpHost,
            @Min(1) @Max(65535) Integer smtpPort,
            @Size(max = 150) String smtpUsername,
            @Size(max = 200) String smtpPassword,
            boolean clearSmtpPassword,
            boolean smtpAuth,
            boolean smtpStarttls,
            boolean smsEnabled,
            @Size(max = 300) String smsGatewayUrl,
            @Size(max = 150) String smsUsername,
            @Size(max = 200) String smsPassword,
            boolean clearSmsPassword,
            @Pattern(regexp = "\\d{1,4}", message = "must be digits, e.g. 1 or 91") String smsCountryCode
    ) {
    }

    public record TestEmailRequest(@NotBlank @Email String to) {
    }
}
