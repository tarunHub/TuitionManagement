package com.smartstart.portal.calendar.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.smartstart.portal.notification.entity.NotificationChannel;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

public final class ClassCalendarDtos {

    private ClassCalendarDtos() {
    }

    public record SessionRequest(
            @NotNull Long classGroupId,
            @NotNull LocalDate date,
            @NotNull @JsonFormat(pattern = "HH:mm") LocalTime startTime,
            @NotNull @JsonFormat(pattern = "HH:mm") LocalTime endTime,
            @Size(max = 150) String location,
            @Size(max = 500) String notes
    ) {
    }

    public record SessionResponse(
            Long id,
            Long classGroupId,
            String classGroupName,
            LocalDate date,
            @JsonFormat(pattern = "HH:mm") LocalTime startTime,
            @JsonFormat(pattern = "HH:mm") LocalTime endTime,
            String location,
            String notes
    ) {
    }

    /** "Every Tue and Thu, 17:00–18:00" for one batch and month; days that already have a class are left alone. */
    public record RepeatWeeklyRequest(
            @NotNull Long classGroupId,
            @NotNull @Pattern(regexp = "\\d{4}-\\d{2}", message = "must be YYYY-MM") String month,
            @NotEmpty List<DayOfWeek> weekdays,
            @NotNull @JsonFormat(pattern = "HH:mm") LocalTime startTime,
            @NotNull @JsonFormat(pattern = "HH:mm") LocalTime endTime,
            @Size(max = 150) String location
    ) {
    }

    public record RepeatWeeklyResult(int created, int skipped) {
    }

    /**
     * Batches to send the month's schedule for (empty or null = every batch with classes) and how: EMAIL, SMS or
     * both (empty or null = EMAIL).
     */
    public record SendScheduleRequest(
            @NotNull @Pattern(regexp = "\\d{4}-\\d{2}", message = "must be YYYY-MM") String month,
            List<Long> classGroupIds,
            Set<NotificationChannel> channels
    ) {
    }

    public record ReminderSettingsRequest(
            boolean enabled,
            @Min(0) @Max(2) int daysBefore,
            @NotNull @JsonFormat(pattern = "HH:mm") LocalTime sendTime,
            boolean sendEmail,
            boolean sendSms
    ) {
    }

    /** {@code smsAvailable} is false when SMS isn't set up in .env, in which case only email goes out. */
    public record ReminderSettingsResponse(
            boolean enabled,
            int daysBefore,
            @JsonFormat(pattern = "HH:mm") LocalTime sendTime,
            boolean sendEmail,
            boolean sendSms,
            LocalDate lastRunDate,
            boolean smsAvailable
    ) {
    }

    public enum ChangeKind {
        ADDED,
        CHANGED,
        CANCELLED
    }

    /**
     * Tells a batch's parents about one class that was added, changed or cancelled. Sent only when the admin
     * confirms, so it carries the details itself (a cancelled class no longer exists). The previous* fields describe
     * the class before a change.
     */
    public record ClassChangeRequest(
            @NotNull ChangeKind kind,
            @NotNull Long classGroupId,
            @NotNull LocalDate date,
            @NotNull @JsonFormat(pattern = "HH:mm") LocalTime startTime,
            @NotNull @JsonFormat(pattern = "HH:mm") LocalTime endTime,
            @Size(max = 150) String location,
            LocalDate previousDate,
            @JsonFormat(pattern = "HH:mm") LocalTime previousStartTime,
            @JsonFormat(pattern = "HH:mm") LocalTime previousEndTime,
            @Size(max = 150) String previousLocation
    ) {
    }
}
