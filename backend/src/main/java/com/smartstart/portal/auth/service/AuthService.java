package com.smartstart.portal.auth.service;

import com.smartstart.portal.auth.dto.AuthDtos.AdminUserResponse;
import com.smartstart.portal.auth.dto.AuthDtos.ChangePasswordRequest;
import com.smartstart.portal.auth.dto.AuthDtos.LoginRequest;
import com.smartstart.portal.auth.dto.AuthDtos.LoginResponse;
import com.smartstart.portal.auth.entity.AdminUser;
import com.smartstart.portal.auth.repository.AdminUserRepository;
import com.smartstart.portal.common.exception.BadRequestException;
import com.smartstart.portal.common.exception.NotFoundException;
import com.smartstart.portal.common.exception.TooManyRequestsException;
import com.smartstart.portal.security.AdminUserDetailsService;
import com.smartstart.portal.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String INVALID = "Invalid username or password";

    private final AdminUserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttempts;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String username = request.username().trim();
        loginAttempts.lockedFor(username).ifPresent(remaining -> {
            long minutes = Math.max(1, (remaining.toSeconds() + 59) / 60);
            throw new TooManyRequestsException("ACCOUNT_LOCKED",
                    "Too many failed login attempts. Try again in " + minutes + " minute(s).");
        });

        AdminUser user = repository.findByUsernameIgnoreCase(username).orElse(null);
        if (user == null || !user.isEnabled() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            loginAttempts.recordFailure(username);
            log.info("Failed login attempt");
            throw new BadCredentialsException(INVALID);
        }
        loginAttempts.recordSuccess(username);
        JwtService.IssuedToken token = jwtService.issue(user.getUsername(), AdminUserDetailsService.ROLE_ADMIN);
        return new LoginResponse(token.token(), token.expiresAt(), toResponse(user));
    }

    @Transactional(readOnly = true)
    public AdminUserResponse me(String username) {
        return toResponse(find(username));
    }

    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        AdminUser user = find(username);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("INVALID_PASSWORD", "Current password is incorrect", "currentPassword");
        }
        if (request.currentPassword().equals(request.newPassword())) {
            throw new BadRequestException("VALIDATION_FAILED", "New password must differ from the current one", "newPassword");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    }

    private AdminUser find(String username) {
        return repository.findByUsernameIgnoreCase(username).orElseThrow(() -> new NotFoundException("User", username));
    }

    private static AdminUserResponse toResponse(AdminUser user) {
        return new AdminUserResponse(user.getId(), user.getUsername(), user.getFullName(), AdminUserDetailsService.ROLE_ADMIN);
    }
}
