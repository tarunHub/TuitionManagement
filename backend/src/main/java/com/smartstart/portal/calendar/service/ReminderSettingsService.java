package com.smartstart.portal.calendar.service;

import com.smartstart.portal.calendar.dto.ClassCalendarDtos.ReminderSettingsRequest;
import com.smartstart.portal.calendar.dto.ClassCalendarDtos.ReminderSettingsResponse;
import com.smartstart.portal.calendar.entity.ReminderSettings;
import com.smartstart.portal.calendar.repository.ReminderSettingsRepository;
import com.smartstart.portal.notification.entity.NotificationChannel;
import com.smartstart.portal.notification.service.SmsGatewayNotificationSender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/** Automatic class reminder settings, edited on the Class Calendar page. */
@Service
@RequiredArgsConstructor
public class ReminderSettingsService {

    private final ReminderSettingsRepository repository;
    private final SmsGatewayNotificationSender sms;
    private final Clock clock;

    @Transactional(readOnly = true)
    public ReminderSettingsResponse get() {
        return toResponse(load());
    }

    @Transactional
    public ReminderSettingsResponse update(ReminderSettingsRequest r) {
        ReminderSettings s = load();
        s.setEnabled(r.enabled());
        s.setDaysBefore((short) r.daysBefore());
        s.setSendTime(r.sendTime());
        s.setSendEmail(r.sendEmail());
        s.setSendSms(r.sendSms());
        s.setUpdatedAt(clock.instant());
        return toResponse(repository.save(s));
    }

    /**
     * Called once a minute. When reminders are on, the send time has passed and today's run hasn't happened yet,
     * marks today as done and returns the class date to remind about (today + days before). Marking first means a
     * failure can't cause a second batch of messages. If the app was off at the send time, it catches up later the
     * same day.
     */
    @Transactional
    public Optional<Due> claimDueRun() {
        ReminderSettings s = load();
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();
        if (!s.isEnabled() || now.toLocalTime().isBefore(s.getSendTime()) || today.equals(s.getLastRunDate())) {
            return Optional.empty();
        }
        s.setLastRunDate(today);
        repository.save(s);
        return Optional.of(new Due(today.plusDays(s.getDaysBefore()), channels(s)));
    }

    private Set<NotificationChannel> channels(ReminderSettings s) {
        Set<NotificationChannel> out = EnumSet.noneOf(NotificationChannel.class);
        if (s.isSendEmail()) {
            out.add(NotificationChannel.EMAIL);
        }
        if (s.isSendSms() && sms.isConfigured()) {
            out.add(NotificationChannel.SMS);
        }
        return out;
    }

    private ReminderSettings load() {
        return repository.findById(ReminderSettings.ID).orElseThrow(() -> new IllegalStateException("reminder_settings row missing"));
    }

    private ReminderSettingsResponse toResponse(ReminderSettings s) {
        return new ReminderSettingsResponse(s.isEnabled(), s.getDaysBefore(), s.getSendTime(), s.isSendEmail(), s.isSendSms(),
                s.getLastRunDate(), sms.isConfigured());
    }

    public record Due(LocalDate classDate, Set<NotificationChannel> channels) {
    }
}
