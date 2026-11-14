package com.smartstart.portal.notification.service;

import com.smartstart.portal.centre.entity.Centre;
import com.smartstart.portal.centre.service.CurrentCentre;
import com.smartstart.portal.notification.entity.Notification;
import com.smartstart.portal.notification.entity.NotificationChannel;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import java.time.Instant;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Sends one plain-text email per notification. Parents are addressed via BCC so they never see each other's addresses.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotificationSender implements NotificationSender {

    private final CurrentCentre currentCentre;
    /** One mail client per centre, rebuilt when the centre's settings change. */
    private final Map<Long, CachedSender> cache = new ConcurrentHashMap<>();

    private record CachedSender(Instant version, JavaMailSenderImpl sender) {
    }

    /** True when the current centre has entered its email (SMTP) settings. */
    public boolean isConfigured() {
        return currentCentre.get().emailConfigured();
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public void send(Notification notification) throws NotificationDeliveryException {
        List<String> recipients = notification.recipientList();
        if (recipients.isEmpty()) {
            throw new NotificationDeliveryException("No recipients", null);
        }
        Centre centre = currentCentre.get();
        if (!centre.emailConfigured()) {
            throw new NotificationDeliveryException("Email is not set up for this centre (Centre settings → Email)", null);
        }
        try {
            JavaMailSender mailSender = senderFor(centre);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            String from = centre.getMailFrom();
            helper.setFrom(from, centre.getMailFromName() == null || centre.getMailFromName().isBlank() ? centre.getName() : centre.getMailFromName());
            if (recipients.size() == 1) {
                helper.setTo(recipients.getFirst());
            } else {
                helper.setTo(from);
                helper.setBcc(recipients.toArray(String[]::new));
            }
            helper.setSubject(notification.getSubject());
            helper.setText(notification.getBody(), false);
            mailSender.send(message);
            log.info("Sent notification {} to {} recipient(s)", notification.getId(), recipients.size());
        } catch (Exception e) {
            String reason = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            throw new NotificationDeliveryException("Email delivery failed: " + reason, e);
        }
    }

    /** Sends a short test message with the given settings (Centre settings → Send test email). */
    public void sendTest(Centre centre, String to) throws NotificationDeliveryException {
        try {
            JavaMailSenderImpl sender = build(centre);
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(centre.getMailFrom(), centre.getMailFromName() == null || centre.getMailFromName().isBlank() ? centre.getName() : centre.getMailFromName());
            helper.setTo(to);
            helper.setSubject(centre.getName() + ": test email");
            helper.setText("This is a test email from the student portal. Email is set up correctly for " + centre.getName() + ".", false);
            sender.send(message);
        } catch (Exception e) {
            String reason = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            throw new NotificationDeliveryException("Test email failed: " + reason, e);
        }
    }

    private JavaMailSender senderFor(Centre centre) {
        CachedSender cached = cache.get(centre.getId());
        if (cached == null || !cached.version().equals(centre.getUpdatedAt())) {
            cached = new CachedSender(centre.getUpdatedAt(), build(centre));
            cache.put(centre.getId(), cached);
        }
        return cached.sender();
    }

    private static JavaMailSenderImpl build(Centre centre) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(centre.getSmtpHost());
        sender.setPort(centre.getSmtpPort() == null ? 587 : centre.getSmtpPort());
        sender.setDefaultEncoding(StandardCharsets.UTF_8.name());
        if (centre.isSmtpAuth()) {
            sender.setUsername(centre.getSmtpUsername());
            sender.setPassword(centre.getSmtpPassword());
        }
        Properties p = sender.getJavaMailProperties();
        p.put("mail.smtp.auth", String.valueOf(centre.isSmtpAuth()));
        p.put("mail.smtp.starttls.enable", String.valueOf(centre.isSmtpStarttls()));
        p.put("mail.smtp.connectiontimeout", "10000");
        p.put("mail.smtp.timeout", "10000");
        p.put("mail.smtp.writetimeout", "10000");
        return sender;
    }
}
