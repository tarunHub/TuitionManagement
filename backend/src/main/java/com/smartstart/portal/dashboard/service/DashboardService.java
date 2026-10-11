package com.smartstart.portal.dashboard.service;

import com.smartstart.portal.attendance.dto.AttendanceDtos.StudentAttendanceCount;
import com.smartstart.portal.attendance.repository.AttendanceRepository;
import com.smartstart.portal.common.util.Money;
import com.smartstart.portal.dashboard.dto.DashboardDtos.AttendanceCard;
import com.smartstart.portal.dashboard.dto.DashboardDtos.DashboardResponse;
import com.smartstart.portal.dashboard.dto.DashboardDtos.FeedbackMissing;
import com.smartstart.portal.dashboard.dto.DashboardDtos.FeesCard;
import com.smartstart.portal.dashboard.dto.DashboardDtos.MonthSummary;
import com.smartstart.portal.dashboard.dto.DashboardDtos.MonthlySummaryResponse;
import com.smartstart.portal.dashboard.dto.DashboardDtos.OutstandingStudent;
import com.smartstart.portal.dashboard.dto.DashboardDtos.StudentsCard;
import com.smartstart.portal.fee.repository.FeePaymentRepository;
import com.smartstart.portal.fee.repository.MonthlyFeeRepository;
import com.smartstart.portal.fee.service.FeeCalculator;
import com.smartstart.portal.feedback.repository.MonthlyFeedbackRepository;
import com.smartstart.portal.schedule.service.ScheduleCalendar;
import com.smartstart.portal.student.entity.Student;
import com.smartstart.portal.student.entity.StudentStatus;
import com.smartstart.portal.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Dashboard aggregates; every number is computed with aggregate queries (no per-row loading). */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int TOP_OUTSTANDING = 5;

    private final StudentRepository studentRepository;
    private final MonthlyFeeRepository feeRepository;
    private final FeePaymentRepository paymentRepository;
    private final AttendanceRepository attendanceRepository;
    private final MonthlyFeedbackRepository feedbackRepository;
    private final ScheduleCalendar scheduleCalendar;
    private final FeeCalculator calculator;
    private final Clock clock;

    @Transactional(readOnly = true)
    public DashboardResponse dashboard(YearMonth month) {
        StudentsCard students = new StudentsCard(studentRepository.countByStatus(StudentStatus.ACTIVE),
                studentRepository.countByStatus(StudentStatus.INACTIVE));

        LocalDate first = month.atDay(1);
        BigDecimal billed = sumFor(feeRepository.sumDueByMonth(first, first), first);
        BigDecimal collected = sumFor(paymentRepository.sumPaidByFeeMonth(first, first), first);
        FeesCard fees = new FeesCard(billed, collected, calculator.balance(billed, collected),
                calculator.collectionRatePct(billed, collected));

        List<StudentAttendanceCount> counts = attendanceRepository.countByStudent(first, month.atEndOfMonth());
        long present = counts.stream().mapToLong(StudentAttendanceCount::present).sum();
        long absent = counts.stream().mapToLong(StudentAttendanceCount::absent).sum();
        int maxHeld = (int) counts.stream().mapToLong(StudentAttendanceCount::held).max().orElse(0);
        AttendanceCard attendance = new AttendanceCard(maxHeld, present, absent, calculator.attendancePct(present, present + absent));

        LocalDate today = LocalDate.now(clock);
        LocalDate endOfWeek = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        return new DashboardResponse(month.toString(), students, fees, attendance,
                scheduleCalendar.occurrences(today, endOfWeek),
                topOutstanding(),
                new FeedbackMissing(month.toString(), feedbackRepository.countActiveWithoutFeedback(first)));
    }

    @Transactional(readOnly = true)
    public MonthlySummaryResponse monthlySummary(int year) {
        LocalDate from = Year.of(year).atDay(1);
        LocalDate to = Year.of(year).atMonth(12).atDay(1);
        Map<LocalDate, BigDecimal> due = toMap(feeRepository.sumDueByMonth(from, to));
        Map<LocalDate, BigDecimal> paid = toMap(paymentRepository.sumPaidByFeeMonth(from, to));
        List<MonthSummary> months = new ArrayList<>(12);
        BigDecimal totalDue = BigDecimal.ZERO;
        BigDecimal totalPaid = BigDecimal.ZERO;
        for (int m = 1; m <= 12; m++) {
            LocalDate key = LocalDate.of(year, m, 1);
            BigDecimal d = Money.scale(due.get(key));
            BigDecimal p = Money.scale(paid.get(key));
            totalDue = totalDue.add(d);
            totalPaid = totalPaid.add(p);
            months.add(new MonthSummary(YearMonth.of(year, m).toString(), d, p, calculator.balance(d, p)));
        }
        return new MonthlySummaryResponse(year, months,
                new MonthSummary("TOTAL", Money.scale(totalDue), Money.scale(totalPaid), calculator.balance(totalDue, totalPaid)));
    }

    /** Students whose total balance across all months is positive, largest first. */
    private List<OutstandingStudent> topOutstanding() {
        Map<Long, BigDecimal> due = toLongMap(feeRepository.sumDueByStudent());
        Map<Long, BigDecimal> paid = toLongMap(paymentRepository.sumPaidByStudent());
        Map<Long, BigDecimal> balances = new HashMap<>();
        due.forEach((id, d) -> {
            BigDecimal b = calculator.balance(d, paid.get(id));
            if (b.signum() > 0) {
                balances.put(id, b);
            }
        });
        List<Long> top = balances.entrySet().stream()
                .sorted(Map.Entry.<Long, BigDecimal>comparingByValue().reversed())
                .limit(TOP_OUTSTANDING)
                .map(Map.Entry::getKey)
                .toList();
        if (top.isEmpty()) {
            return List.of();
        }
        Map<Long, Student> students = studentRepository.findAllById(top).stream()
                .collect(Collectors.toMap(Student::getId, Function.identity()));
        return top.stream()
                .map(id -> new OutstandingStudent(id, students.get(id).getStudentCode(), students.get(id).getFullName(), balances.get(id)))
                .sorted(Comparator.comparing(OutstandingStudent::balance).reversed())
                .toList();
    }

    private static BigDecimal sumFor(List<Object[]> rows, LocalDate key) {
        return Money.scale(toMap(rows).get(key));
    }

    private static Map<LocalDate, BigDecimal> toMap(List<Object[]> rows) {
        Map<LocalDate, BigDecimal> out = new HashMap<>();
        rows.forEach(r -> out.put((LocalDate) r[0], (BigDecimal) r[1]));
        return out;
    }

    private static Map<Long, BigDecimal> toLongMap(List<Object[]> rows) {
        Map<Long, BigDecimal> out = new HashMap<>();
        rows.forEach(r -> out.put((Long) r[0], (BigDecimal) r[1]));
        return out;
    }
}
