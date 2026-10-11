package com.smartstart.portal.calendar.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

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

    /** Batches to email; empty or null means every batch that has classes in the month. */
    public record EmailScheduleRequest(
            @NotNull @Pattern(regexp = "\\d{4}-\\d{2}", message = "must be YYYY-MM") String month,
            List<Long> classGroupIds
    ) {
    }
}
