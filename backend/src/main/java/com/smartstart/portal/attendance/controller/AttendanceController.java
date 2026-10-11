package com.smartstart.portal.attendance.controller;

import com.smartstart.portal.attendance.dto.AttendanceDtos.AttendanceGridResponse;
import com.smartstart.portal.attendance.dto.AttendanceDtos.AttendanceUpdate;
import com.smartstart.portal.attendance.dto.AttendanceDtos.BulkUpdateResult;
import com.smartstart.portal.attendance.dto.AttendanceDtos.StudentAttendanceHistory;
import com.smartstart.portal.attendance.service.AttendanceExportService;
import com.smartstart.portal.attendance.service.AttendanceService;
import com.smartstart.portal.common.util.FileResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.List;

@Tag(name = "Attendance")
@Validated
@RestController
@RequestMapping("/api/v1/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService service;
    private final AttendanceExportService exportService;

    @Operation(summary = "Monthly grid: active students × days, with totals")
    @GetMapping
    public AttendanceGridResponse grid(@Parameter(example = "2026-08") @RequestParam YearMonth month,
                                       @RequestParam(required = false) Long groupId) {
        return service.grid(month, groupId);
    }

    @Operation(summary = "Bulk upsert; a null status removes the row (no class that day)")
    @PutMapping("/bulk")
    public BulkUpdateResult bulk(@RequestBody List<@Valid AttendanceUpdate> updates) {
        return service.bulkUpdate(updates);
    }

    @Operation(summary = "Export the month to Excel")
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@Parameter(example = "2026-08") @RequestParam YearMonth month,
                                         @RequestParam(required = false) Long groupId) {
        return FileResponses.excel(exportService.export(month, groupId), "attendance-" + month + ".xlsx");
    }

    @Operation(summary = "Attendance history for one student")
    @GetMapping("/students/{studentId}")
    public StudentAttendanceHistory history(@PathVariable Long studentId) {
        return service.history(studentId);
    }
}
