package com.smartstart.portal.attendance.dto;

import com.smartstart.portal.attendance.entity.AttendanceStatus;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class AttendanceDtos {

    private AttendanceDtos() {
    }

    /** One cell change; {@code status = null} removes the row (no class that day). */
    public record AttendanceUpdate(@NotNull Long studentId, @NotNull LocalDate date, AttendanceStatus status) {
    }

    public record BulkUpdateResult(int created, int updated, int removed) {
    }

    public record AttendanceCell(Long studentId, LocalDate date, AttendanceStatus status) {
    }

    public record StudentAttendanceCount(Long studentId, Long present, Long absent) {
        public long held() {
            return present + absent;
        }
    }

    public record GridDay(int day, LocalDate date, String dayLetter, boolean weekend, boolean future) {
    }

    public record GridRow(
            Long studentId,
            String studentCode,
            String fullName,
            String classGroupName,
            /* index 0 = day 1; null = no class */
            List<AttendanceStatus> cells,
            /* index 0 = day 1; true = the student's batch has a class that day on the class calendar */
            List<Boolean> scheduled,
            int present,
            int absent,
            int held,
            BigDecimal attendancePct
    ) {
    }

    public record GridTotals(int present, int absent, int held, BigDecimal attendancePct) {
    }

    public record AttendanceGridResponse(
            String month,
            LocalDate today,
            List<GridDay> days,
            List<GridRow> rows,
            List<Integer> presentPerDay,
            GridTotals totals
    ) {
    }

    public record AttendanceHistoryEntry(LocalDate date, AttendanceStatus status) {
    }

    public record MonthlyAttendanceSummary(String month, int present, int absent, int held, BigDecimal attendancePct) {
    }

    public record StudentAttendanceHistory(List<MonthlyAttendanceSummary> months, List<AttendanceHistoryEntry> entries) {
    }
}
