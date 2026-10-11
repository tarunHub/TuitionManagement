package com.smartstart.portal.dashboard.dto;

import com.smartstart.portal.schedule.dto.ScheduleDtos.ClassOccurrence;

import java.math.BigDecimal;
import java.util.List;

public final class DashboardDtos {

    private DashboardDtos() {
    }

    public record StudentsCard(long active, long inactive) {
    }

    /** {@code collectionRatePct} is null when nothing was billed (shown as "—"). */
    public record FeesCard(BigDecimal billed, BigDecimal collected, BigDecimal outstanding, BigDecimal collectionRatePct) {
    }

    /** {@code averageAttendancePct} is null when no classes were held. */
    public record AttendanceCard(int classesHeldMax, long totalPresent, long totalAbsent, BigDecimal averageAttendancePct) {
    }

    public record OutstandingStudent(Long studentId, String studentCode, String fullName, BigDecimal balance) {
    }

    public record FeedbackMissing(String month, long count) {
    }

    public record DashboardResponse(
            String month,
            StudentsCard students,
            FeesCard fees,
            AttendanceCard attendance,
            List<ClassOccurrence> upcomingClasses,
            List<OutstandingStudent> outstandingStudents,
            FeedbackMissing feedbackMissing
    ) {
    }

    public record MonthSummary(String month, BigDecimal billed, BigDecimal collected, BigDecimal outstanding) {
    }

    public record MonthlySummaryResponse(int year, List<MonthSummary> months, MonthSummary total) {
    }
}
