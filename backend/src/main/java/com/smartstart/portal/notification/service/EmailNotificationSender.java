package com.smartstart.portal.notification.service;

import com.smartstart.portal.config.AppProperties;
import com.smartstart.portal.notification.entity.Notification;
import com.smartstart.portal.notification.entity.NotificationChannel;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
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

    private final JavaMailSender mailSender;
    private final AppProperties props;

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
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            String from = props.mail().from();
            helper.setFrom(from, props.mail().fromName());
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
}
