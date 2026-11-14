package com.smartstart.portal.centre.controller;

import com.smartstart.portal.centre.dto.CentreDtos.MessagingSettingsRequest;
import com.smartstart.portal.centre.dto.CentreDtos.MessagingSettingsResponse;
import com.smartstart.portal.centre.dto.CentreDtos.TestEmailRequest;
import com.smartstart.portal.centre.service.CentreSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The current centre's email and SMS settings. */
@Tag(name = "Centre settings")
@RestController
@RequestMapping("/api/v1/centre/settings")
@RequiredArgsConstructor
public class CentreSettingsController {

    private final CentreSettingsService service;

    @GetMapping
    public MessagingSettingsResponse get() {
        return service.get();
    }

    @PutMapping
    public MessagingSettingsResponse update(@Valid @RequestBody MessagingSettingsRequest request) {
        return service.update(request);
    }

    @Operation(summary = "Send a test email with the saved settings")
    @PostMapping("/test-email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void testEmail(@Valid @RequestBody TestEmailRequest request) {
        service.sendTestEmail(request.to());
    }
}
