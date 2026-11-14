package com.smartstart.portal.centre.service;

import com.smartstart.portal.centre.dto.CentreDtos.MessagingSettingsRequest;
import com.smartstart.portal.centre.dto.CentreDtos.MessagingSettingsResponse;
import com.smartstart.portal.centre.entity.Centre;
import com.smartstart.portal.centre.repository.CentreRepository;
import com.smartstart.portal.centre.tenancy.TenantContext;
import com.smartstart.portal.common.exception.BadRequestException;
import com.smartstart.portal.notification.service.EmailNotificationSender;
import com.smartstart.portal.notification.service.NotificationSender.NotificationDeliveryException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The current centre's own email (SMTP) and SMS gateway settings. */
@Service
@RequiredArgsConstructor
public class CentreSettingsService {

    private final CentreRepository repository;
    private final EmailNotificationSender email;

    @Transactional(readOnly = true)
    public MessagingSettingsResponse get() {
        return toResponse(load());
    }

    @Transactional
    public MessagingSettingsResponse update(MessagingSettingsRequest r) {
        Centre c = load();
        c.setMailFrom(blankToNull(r.mailFrom()));
        c.setMailFromName(blankToNull(r.mailFromName()));
        c.setSmtpHost(blankToNull(r.smtpHost()));
        c.setSmtpPort(r.smtpPort());
        c.setSmtpUsername(blankToNull(r.smtpUsername()));
        if (r.clearSmtpPassword()) {
            c.setSmtpPassword(null);
        } else if (r.smtpPassword() != null && !r.smtpPassword().isBlank()) {
            c.setSmtpPassword(r.smtpPassword());
        }
        c.setSmtpAuth(r.smtpAuth());
        c.setSmtpStarttls(r.smtpStarttls());
        c.setSmsEnabled(r.smsEnabled());
        c.setSmsGatewayUrl(blankToNull(r.smsGatewayUrl()));
        c.setSmsUsername(blankToNull(r.smsUsername()));
        if (r.clearSmsPassword()) {
            c.setSmsPassword(null);
        } else if (r.smsPassword() != null && !r.smsPassword().isBlank()) {
            c.setSmsPassword(r.smsPassword());
        }
        c.setSmsCountryCode(r.smsCountryCode() == null || r.smsCountryCode().isBlank() ? "1" : r.smsCountryCode().strip());
        if (r.smsEnabled() && c.getSmsGatewayUrl() == null) {
            throw new BadRequestException("VALIDATION_FAILED", "is required to turn SMS on", "smsGatewayUrl");
        }
        return toResponse(repository.saveAndFlush(c));
    }

    /** Sends a test email with the saved settings. */
    @Transactional(readOnly = true)
    public void sendTestEmail(String to) {
        Centre c = load();
        if (!c.emailConfigured()) {
            throw new BadRequestException("EMAIL_NOT_CONFIGURED", "Save the From address, SMTP server and port first");
        }
        try {
            email.sendTest(c, to.strip());
        } catch (NotificationDeliveryException e) {
            throw new BadRequestException("TEST_EMAIL_FAILED", e.getMessage());
        }
    }

    private Centre load() {
        long id = TenantContext.require();
        return repository.findById(id).orElseThrow(() -> new IllegalStateException("Centre " + id + " not found"));
    }

    private static MessagingSettingsResponse toResponse(Centre c) {
        return new MessagingSettingsResponse(c.getName(), c.getStudentCodePrefix(),
                c.getMailFrom(), c.getMailFromName(), c.getSmtpHost(), c.getSmtpPort(), c.getSmtpUsername(),
                c.getSmtpPassword() != null && !c.getSmtpPassword().isEmpty(), c.isSmtpAuth(), c.isSmtpStarttls(), c.emailConfigured(),
                c.isSmsEnabled(), c.getSmsGatewayUrl(), c.getSmsUsername(),
                c.getSmsPassword() != null && !c.getSmsPassword().isEmpty(), c.getSmsCountryCode(), c.smsConfigured());
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.strip();
    }
}
