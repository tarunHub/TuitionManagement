package com.smartstart.portal.notification.service;

import com.smartstart.portal.centre.service.EachCentre;
import com.smartstart.portal.calendar.service.ReminderSettingsService;
import com.smartstart.portal.notification.dto.NotificationDtos.BulkSendResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Clock;
import java.time.YearMonth;

/**
 * Automatic notification jobs. Each can be switched off (app.jobs.*.enabled) and re-timed (app.jobs.*.cron).
 * All crons run in America/Toronto.
 */
@Configuration
public class NotificationJobs {

    @Bean
    @ConditionalOnProperty(name = "app.jobs.dispatch.enabled", havingValue = "true")
    public ScheduledDispatchJob scheduledDispatchJob(NotificationService service, EachCentre eachCentre) {
        return new ScheduledDispatchJob(service, eachCentre);
    }

    @Bean
    @ConditionalOnProperty(name = "app.jobs.class-reminder.enabled", havingValue = "true")
    public ClassReminderJob classReminderJob(NotificationService service, ReminderSettingsService settings, EachCentre eachCentre) {
        return new ClassReminderJob(service, settings, eachCentre);
    }

    @Bean
    @ConditionalOnProperty(name = "app.jobs.fee-reminder.enabled", havingValue = "true")
    public FeeReminderJob feeReminderJob(NotificationService service, Clock clock, EachCentre eachCentre) {
        return new FeeReminderJob(service, clock, eachCentre);
    }

    /** Sends notifications whose scheduled time has passed. */
    @Slf4j
    @RequiredArgsConstructor
    public static class ScheduledDispatchJob {
        private final NotificationService service;
        private final EachCentre eachCentre;

        @Scheduled(cron = "${app.jobs.dispatch.cron}", zone = "America/Toronto")
        public void run() {
            eachCentre.run("Scheduled dispatch", centre -> {
                for (Long id : service.dueScheduledIds()) {
                    try {
                        service.sendIfStillScheduled(id);
                    } catch (RuntimeException e) {
                        log.error("Dispatch of notification {} failed", id, e);
                    }
                }
            });
        }
    }

    /**
     * Automatic class reminders. Checks once a minute against the settings on the Class Calendar page (on/off, time,
     * day of the class or days before, email/SMS) and sends once per day.
     */
    @Slf4j
    @RequiredArgsConstructor
    public static class ClassReminderJob {
        private final NotificationService service;
        private final ReminderSettingsService settings;
        private final EachCentre eachCentre;

        @Scheduled(cron = "0 * * * * *", zone = "America/Toronto")
        public void run() {
            eachCentre.run("Class reminders", centre -> settings.claimDueRun().ifPresent(due -> {
                if (due.channels().isEmpty()) {
                    log.info("Centre {}: class reminders for {} skipped, email and SMS are both switched off", centre, due.classDate());
                    return;
                }
                BulkSendResult result = service.classReminders(due.classDate(), due.channels());
                log.info("Centre {}: class reminders for {} ({}): {} created, {} sent, {} failed", centre, due.classDate(),
                        due.channels(), result.created(), result.sent(), result.failed());
            }));
        }
    }

    /** Monthly reminders (default the 5th) for the previous month's unpaid/partial fees. */
    @Slf4j
    @RequiredArgsConstructor
    public static class FeeReminderJob {
        private final NotificationService service;
        private final Clock clock;
        private final EachCentre eachCentre;

        @Scheduled(cron = "${app.jobs.fee-reminder.cron}", zone = "America/Toronto")
        public void run() {
            YearMonth previous = YearMonth.now(clock).minusMonths(1);
            eachCentre.run("Fee reminders", centre -> {
                BulkSendResult result = service.feeRemindersForMonth(previous);
                log.info("Centre {}: fee reminders for {}: {} created, {} sent, {} failed", centre, previous,
                        result.created(), result.sent(), result.failed());
            });
        }
    }
}
