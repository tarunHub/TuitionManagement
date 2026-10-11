package com.smartstart.portal.notification.service;

import com.smartstart.portal.notification.dto.NotificationDtos.BulkSendResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Automatic notification jobs. Each can be switched off (app.jobs.*.enabled) and re-timed (app.jobs.*.cron).
 * All crons run in America/Toronto.
 */
@Configuration
public class NotificationJobs {

    @Bean
    @ConditionalOnProperty(name = "app.jobs.dispatch.enabled", havingValue = "true")
    public ScheduledDispatchJob scheduledDispatchJob(NotificationService service) {
        return new ScheduledDispatchJob(service);
    }

    @Bean
    @ConditionalOnProperty(name = "app.jobs.class-reminder.enabled", havingValue = "true")
    public ClassReminderJob classReminderJob(NotificationService service, Clock clock) {
        return new ClassReminderJob(service, clock);
    }

    @Bean
    @ConditionalOnProperty(name = "app.jobs.fee-reminder.enabled", havingValue = "true")
    public FeeReminderJob feeReminderJob(NotificationService service, Clock clock) {
        return new FeeReminderJob(service, clock);
    }

    /** Sends notifications whose scheduled time has passed. */
    @Slf4j
    @RequiredArgsConstructor
    public static class ScheduledDispatchJob {
        private final NotificationService service;

        @Scheduled(cron = "${app.jobs.dispatch.cron}", zone = "America/Toronto")
        public void run() {
            for (Long id : service.dueScheduledIds()) {
                try {
                    service.sendIfStillScheduled(id);
                } catch (RuntimeException e) {
                    log.error("Dispatch of notification {} failed", id, e);
                }
            }
        }
    }

    /** Day-before class reminders (default 6 PM). */
    @Slf4j
    @RequiredArgsConstructor
    public static class ClassReminderJob {
        private final NotificationService service;
        private final Clock clock;

        @Scheduled(cron = "${app.jobs.class-reminder.cron}", zone = "America/Toronto")
        public void run() {
            LocalDate tomorrow = LocalDate.now(clock).plusDays(1);
            BulkSendResult result = service.classReminders(tomorrow);
            log.info("Class reminders for {}: {} created, {} sent, {} failed", tomorrow, result.created(), result.sent(), result.failed());
        }
    }

    /** Monthly reminders (default the 5th) for the previous month's unpaid/partial fees. */
    @Slf4j
    @RequiredArgsConstructor
    public static class FeeReminderJob {
        private final NotificationService service;
        private final Clock clock;

        @Scheduled(cron = "${app.jobs.fee-reminder.cron}", zone = "America/Toronto")
        public void run() {
            YearMonth previous = YearMonth.now(clock).minusMonths(1);
            BulkSendResult result = service.feeRemindersForMonth(previous);
            log.info("Fee reminders for {}: {} created, {} sent, {} failed", previous, result.created(), result.sent(), result.failed());
        }
    }
}
