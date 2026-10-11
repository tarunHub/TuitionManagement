package com.smartstart.portal.auth.controller;

import com.smartstart.portal.auth.dto.AuthDtos.AdminUserResponse;
import com.smartstart.portal.auth.dto.AuthDtos.ChangePasswordRequest;
import com.smartstart.portal.auth.dto.AuthDtos.LoginRequest;
import com.smartstart.portal.auth.dto.AuthDtos.LoginResponse;
import com.smartstart.portal.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Log in and receive a JWT (8 hour expiry)")
    @SecurityRequirements
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @Operation(summary = "Current admin")
    @GetMapping("/me")
    public AdminUserResponse me(@AuthenticationPrincipal UserDetails user) {
        return authService.me(user.getUsername());
    }

    @Operation(summary = "Change the current admin's password")
    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal UserDetails user,
                                               @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(user.getUsername(), request);
        return ResponseEntity.noContent().build();
    }
}
