package com.smartstart.portal.schedule.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.smartstart.portal.schedule.entity.ScheduleDay;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public final class ScheduleDtos {

    private ScheduleDtos() {
    }

    public record CreateTermRequest(
            @NotBlank @Size(max = 100) String name,
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate,
            @Size(max = 150) String calendarName,
            Boolean active
    ) implements TermFields {
    }

    public record UpdateTermRequest(
            @NotBlank @Size(max = 100) String name,
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate,
            @Size(max = 150) String calendarName,
            Boolean active
    ) implements TermFields {
    }

    public interface TermFields {
        String name();

        LocalDate startDate();

        LocalDate endDate();

        String calendarName();

        Boolean active();
    }

    public record TermResponse(Long id, String name, LocalDate startDate, LocalDate endDate, String calendarName,
                               boolean active, Instant createdAt, Instant updatedAt) {
    }

    public record CreateSlotRequest(
            Long termId,
            @NotNull Long classGroupId,
            @NotNull ScheduleDay dayOfWeek,
            @NotNull @JsonFormat(pattern = "HH:mm") LocalTime startTime,
            @NotNull @JsonFormat(pattern = "HH:mm") LocalTime endTime,
            @Size(max = 150) String subject,
            @Size(max = 150) String location,
            @Size(max = 100) String teacher,
            @Size(max = 4000) String extraEmails,
            @Size(max = 2000) String notes
    ) implements SlotFields {
    }

    public record UpdateSlotRequest(
            Long termId,
            @NotNull Long classGroupId,
            @NotNull ScheduleDay dayOfWeek,
            @NotNull @JsonFormat(pattern = "HH:mm") LocalTime startTime,
            @NotNull @JsonFormat(pattern = "HH:mm") LocalTime endTime,
            @Size(max = 150) String subject,
            @Size(max = 150) String location,
            @Size(max = 100) String teacher,
            @Size(max = 4000) String extraEmails,
            @Size(max = 2000) String notes
    ) implements SlotFields {
    }

    public interface SlotFields {
        Long termId();

        Long classGroupId();

        ScheduleDay dayOfWeek();

        LocalTime startTime();

        LocalTime endTime();

        String subject();

        String location();

        String teacher();

        String extraEmails();

        String notes();
    }

    public record SlotResponse(
            Long id,
            Long termId,
            String termName,
            Long classGroupId,
            String classGroupName,
            ScheduleDay dayOfWeek,
            @JsonFormat(pattern = "HH:mm") LocalTime startTime,
            @JsonFormat(pattern = "HH:mm") LocalTime endTime,
            String subject,
            String location,
            String teacher,
            String extraEmails,
            String notes,
            String googleEventId,
            int recipientCount,
            List<Long> overlappingSlotIds,
            String overlapWarning
    ) {
    }

    public record RecipientsResponse(Long slotId, List<String> emails, int fromGroup, int extra) {
    }

    public record ClassOccurrence(
            Long slotId,
            LocalDate date,
            ScheduleDay dayOfWeek,
            @JsonFormat(pattern = "HH:mm") LocalTime startTime,
            @JsonFormat(pattern = "HH:mm") LocalTime endTime,
            Long classGroupId,
            String classGroupName,
            String subject,
            String location,
            String teacher
    ) {
    }

    public record GoogleStatusResponse(boolean enabled, String message) {
    }

    public record SyncResult(String calendarName, int created, int updated, int skipped, List<String> errors) {
    }
}
