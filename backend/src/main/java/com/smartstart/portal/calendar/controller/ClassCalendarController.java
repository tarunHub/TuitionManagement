package com.smartstart.portal.calendar.controller;

import com.smartstart.portal.calendar.dto.ClassCalendarDtos.SendScheduleRequest;
import com.smartstart.portal.calendar.dto.ClassCalendarDtos.ClassChangeRequest;
import com.smartstart.portal.calendar.dto.ClassCalendarDtos.ReminderSettingsRequest;
import com.smartstart.portal.calendar.dto.ClassCalendarDtos.ReminderSettingsResponse;
import com.smartstart.portal.calendar.dto.ClassCalendarDtos.RepeatWeeklyRequest;
import com.smartstart.portal.calendar.dto.ClassCalendarDtos.RepeatWeeklyResult;
import com.smartstart.portal.calendar.dto.ClassCalendarDtos.SessionRequest;
import com.smartstart.portal.calendar.dto.ClassCalendarDtos.SessionResponse;
import com.smartstart.portal.calendar.service.ClassSessionService;
import com.smartstart.portal.calendar.service.ReminderSettingsService;
import com.smartstart.portal.notification.dto.NotificationDtos.BulkSendResult;
import com.smartstart.portal.notification.dto.NotificationDtos.MessagePreview;
import com.smartstart.portal.notification.dto.NotificationDtos.SendReviewedRequest;
import com.smartstart.portal.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Tag(name = "Class calendar")
@RestController
@RequestMapping("/api/v1/class-calendar")
@RequiredArgsConstructor
public class ClassCalendarController {

    private final ClassSessionService service;
    private final NotificationService notifications;
    private final ReminderSettingsService reminderSettings;
    private final Clock clock;

    @Operation(summary = "Class days in a month, optionally for one batch")
    @GetMapping
    public List<SessionResponse> list(@Parameter(example = "2026-10") @RequestParam YearMonth month,
                                      @RequestParam(required = false) Long groupId) {
        return service.list(month, groupId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SessionResponse create(@Valid @RequestBody SessionRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public SessionResponse update(@PathVariable Long id, @Valid @RequestBody SessionRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @Operation(summary = "Add a class on the chosen weekdays of a month (days that already have one are skipped)")
    @PostMapping("/repeat-weekly")
    public RepeatWeeklyResult repeatWeekly(@Valid @RequestBody RepeatWeeklyRequest request) {
        return service.repeatWeekly(request);
    }

    @Operation(summary = "Send each batch's class dates for the month to its parents by email and/or SMS")
    @PostMapping("/send-schedule")
    public BulkSendResult sendSchedule(@Valid @RequestBody SendScheduleRequest request) {
        return notifications.monthlySchedule(YearMonth.parse(request.month()), request.classGroupIds(), request.channels());
    }

    @Operation(summary = "Automatic class reminder settings")
    @GetMapping("/reminder-settings")
    public ReminderSettingsResponse reminderSettings() {
        return reminderSettings.get();
    }

    @PutMapping("/reminder-settings")
    public ReminderSettingsResponse updateReminderSettings(@Valid @RequestBody ReminderSettingsRequest request) {
        return reminderSettings.update(request);
    }

    @Operation(summary = "Tell a batch's parents that a class was added, changed or cancelled (after the admin confirms)")
    @PostMapping("/notify-change")
    public BulkSendResult notifyChange(@Valid @RequestBody ClassChangeRequest request) {
        return notifications.classChange(request);
    }

    @Operation(summary = "Send today's class reminders now")
    @PostMapping("/remind-today")
    public BulkSendResult remindToday() {
        return notifications.classReminders(LocalDate.now(clock));
    }

    @Operation(summary = "Send the class reminder (email + SMS to opted-in parents) for one class now")
    @PostMapping("/{id}/remind")
    public BulkSendResult remind(@PathVariable Long id) {
        return notifications.classReminder(id);
    }

    // Review before sending: each preview returns the exact messages (per student and channel) without sending
    // anything; the admin unticks students or edits text, then posts what is left to /send-messages.

    @Operation(summary = "Preview the monthly schedule messages without sending")
    @PostMapping("/send-schedule/preview")
    public MessagePreview previewSchedule(@Valid @RequestBody SendScheduleRequest request) {
        return notifications.monthlyScheduleDrafts(YearMonth.parse(request.month()), request.classGroupIds(), request.channels());
    }

    @Operation(summary = "Preview a class change notice without sending")
    @PostMapping("/notify-change/preview")
    public MessagePreview previewChange(@Valid @RequestBody ClassChangeRequest request) {
        return notifications.classChangeDrafts(request);
    }

    @Operation(summary = "Preview today's class reminders without sending")
    @PostMapping("/remind-today/preview")
    public MessagePreview previewRemindToday() {
        return notifications.classReminderDrafts(LocalDate.now(clock));
    }

    @Operation(summary = "Preview the reminder for one class without sending")
    @PostMapping("/{id}/remind/preview")
    public MessagePreview previewRemind(@PathVariable Long id) {
        return notifications.classReminderDrafts(id);
    }

    @Operation(summary = "Send reviewed messages (recipients are taken from each student's record)")
    @PostMapping("/send-messages")
    public BulkSendResult sendMessages(@Valid @RequestBody SendReviewedRequest request) {
        return notifications.sendReviewed(request.messages());
    }
}
