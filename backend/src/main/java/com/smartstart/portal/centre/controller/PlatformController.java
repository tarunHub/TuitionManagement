package com.smartstart.portal.centre.controller;

import com.smartstart.portal.centre.dto.CentreDtos.CentreRequest;
import com.smartstart.portal.centre.dto.CentreDtos.CentreResponse;
import com.smartstart.portal.centre.dto.CentreDtos.CreateCentreRequest;
import com.smartstart.portal.centre.dto.CentreDtos.ResetPasswordRequest;
import com.smartstart.portal.centre.dto.CentreDtos.UpdateUserRequest;
import com.smartstart.portal.centre.dto.CentreDtos.UserRequest;
import com.smartstart.portal.centre.dto.CentreDtos.UserResponse;
import com.smartstart.portal.centre.service.PlatformService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Platform admin only (see SecurityConfig): onboard centres and manage every login. */
@Tag(name = "Platform")
@RestController
@RequestMapping("/api/v1/platform")
@RequiredArgsConstructor
public class PlatformController {

    private final PlatformService service;

    @GetMapping("/centres")
    public List<CentreResponse> centres() {
        return service.listCentres();
    }

    @Operation(summary = "Onboard a centre, optionally with its first staff login")
    @PostMapping("/centres")
    @ResponseStatus(HttpStatus.CREATED)
    public CentreResponse createCentre(@Valid @RequestBody CreateCentreRequest request) {
        return service.createCentre(request);
    }

    @GetMapping("/centres/{id}")
    public CentreResponse centre(@PathVariable Long id) {
        return service.getCentre(id);
    }

    @Operation(summary = "Edit a centre's details or switch it on/off (staff of an inactive centre can't sign in)")
    @PutMapping("/centres/{id}")
    public CentreResponse updateCentre(@PathVariable Long id, @Valid @RequestBody CentreRequest request) {
        return service.updateCentre(id, request);
    }

    @GetMapping("/centres/{id}/users")
    public List<UserResponse> centreUsers(@PathVariable Long id) {
        return service.centreUsers(id);
    }

    @PostMapping("/centres/{id}/users")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse addCentreUser(@PathVariable Long id, @Valid @RequestBody UserRequest request) {
        return service.addCentreUser(id, request);
    }

    @GetMapping("/admins")
    public List<UserResponse> admins() {
        return service.platformAdmins();
    }

    @PostMapping("/admins")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse addAdmin(@Valid @RequestBody UserRequest request) {
        return service.addPlatformAdmin(request);
    }

    @PutMapping("/users/{id}")
    public UserResponse updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return service.updateUser(id, request);
    }

    @PostMapping("/users/{id}/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordRequest request) {
        service.resetPassword(id, request);
    }

    @DeleteMapping("/users/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Long id) {
        service.deleteUser(id);
    }
}
