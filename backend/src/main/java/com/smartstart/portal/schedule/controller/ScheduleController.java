package com.smartstart.portal.schedule.controller;

import com.smartstart.portal.schedule.dto.ScheduleDtos.ClassOccurrence;
import com.smartstart.portal.schedule.dto.ScheduleDtos.CreateSlotRequest;
import com.smartstart.portal.schedule.dto.ScheduleDtos.GoogleStatusResponse;
import com.smartstart.portal.schedule.dto.ScheduleDtos.RecipientsResponse;
import com.smartstart.portal.schedule.dto.ScheduleDtos.SlotResponse;
import com.smartstart.portal.schedule.dto.ScheduleDtos.SyncResult;
import com.smartstart.portal.schedule.dto.ScheduleDtos.UpdateSlotRequest;
import com.smartstart.portal.schedule.service.CalendarSyncService;
import com.smartstart.portal.schedule.service.ScheduleCalendar;
import com.smartstart.portal.schedule.service.ScheduleSlotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Schedule")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleSlotService slotService;
    private final ScheduleCalendar scheduleCalendar;
    private final CalendarSyncService calendarSyncService;

    @Operation(summary = "Slots for a term (defaults to the active term)")
    @GetMapping("/schedule-slots")
    public List<SlotResponse> list(@RequestParam(required = false) Long termId) {
        return slotService.list(termId);
    }

    @PostMapping("/schedule-slots")
    @ResponseStatus(HttpStatus.CREATED)
    public SlotResponse create(@Valid @RequestBody CreateSlotRequest request) {
        return slotService.create(request);
    }

    @PutMapping("/schedule-slots/{id}")
    public SlotResponse update(@PathVariable Long id, @Valid @RequestBody UpdateSlotRequest request) {
        return slotService.update(id, request);
    }

    @Operation(summary = "Delete a slot (and its Google Calendar event when synced)")
    @DeleteMapping("/schedule-slots/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        slotService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Resolved recipient emails for a slot")
    @GetMapping("/schedule-slots/{id}/recipients")
    public RecipientsResponse recipients(@PathVariable Long id) {
        return slotService.recipients(id);
    }

    @Operation(summary = "Dated class occurrences in a range")
    @GetMapping("/schedule/occurrences")
    public List<ClassOccurrence> occurrences(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return scheduleCalendar.occurrences(from, to);
    }

    @Operation(summary = "Whether Google Calendar sync is configured")
    @GetMapping("/schedule/google-status")
    public GoogleStatusResponse googleStatus() {
        return calendarSyncService.status();
    }

    @Operation(summary = "Sync the active term's slots to Google Calendar (501 when not configured)")
    @PostMapping("/schedule/sync-google")
    public SyncResult syncGoogle() {
        return calendarSyncService.syncActiveTerm();
    }
}
