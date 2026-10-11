package com.smartstart.portal.fee.service;

import com.smartstart.portal.attendance.dto.AttendanceDtos.StudentAttendanceCount;
import com.smartstart.portal.attendance.repository.AttendanceRepository;
import com.smartstart.portal.common.exception.BadRequestException;
import com.smartstart.portal.common.exception.NotFoundException;
import com.smartstart.portal.common.util.Money;
import com.smartstart.portal.fee.dto.FeeDtos.CreatePaymentRequest;
import com.smartstart.portal.fee.dto.FeeDtos.FeeListResponse;
import com.smartstart.portal.fee.dto.FeeDtos.FeeRowResponse;
import com.smartstart.portal.fee.dto.FeeDtos.FeeTotals;
import com.smartstart.portal.fee.dto.FeeDtos.GenerateFeesResponse;
import com.smartstart.portal.fee.dto.FeeDtos.PaymentFields;
import com.smartstart.portal.fee.dto.FeeDtos.PaymentResponse;
import com.smartstart.portal.fee.dto.FeeDtos.UpdateMonthlyFeeRequest;
import com.smartstart.portal.fee.dto.FeeDtos.UpdatePaymentRequest;
import com.smartstart.portal.fee.entity.FeePayment;
import com.smartstart.portal.fee.entity.FeeStatus;
import com.smartstart.portal.fee.entity.MonthlyFee;
import com.smartstart.portal.fee.mapper.FeePaymentMapper;
import com.smartstart.portal.fee.repository.FeePaymentRepository;
import com.smartstart.portal.fee.repository.MonthlyFeeRepository;
import com.smartstart.portal.schedule.service.ScheduleCalendar;
import com.smartstart.portal.student.entity.Student;
import com.smartstart.portal.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeeService {

    private final MonthlyFeeRepository feeRepository;
    private final FeePaymentRepository paymentRepository;
    private final StudentRepository studentRepository;
    private final AttendanceRepository attendanceRepository;
    private final ScheduleCalendar scheduleCalendar;
    private final FeeCalculator calculator;
    private final FeePaymentMapper paymentMapper;
    private final Clock clock;

    @Transactional(readOnly = true)
    public FeeListResponse list(YearMonth month, FeeStatus status, Long groupId) {
        List<MonthlyFee> fees = feeRepository.findByMonthWithStudent(month.atDay(1));
        List<MonthlyFee> inGroup = fees.stream()
                .filter(f -> groupId == null || (f.getStudent().getClassGroup() != null
                        && groupId.equals(f.getStudent().getClassGroup().getId())))
                .toList();
        List<FeeRowResponse> rows = toRows(inGroup).stream()
                .filter(r -> status == null || r.status() == status)
                .toList();
        Set<Long> billed = fees.stream().map(f -> f.getStudent().getId()).collect(Collectors.toSet());
        int missing = (int) studentRepository.findAllActive().stream().filter(s -> !billed.contains(s.getId())).count();
        return new FeeListResponse(month.toString(), rows, totals(rows), missing);
    }

    /**
     * Rule 5: creates or refreshes one fee per ACTIVE student for the month. Idempotent; never touches payments.
     * Existing rows keep their rate snapshot (rule 3) and any manually entered classes_scheduled.
     */
    @Transactional
    public GenerateFeesResponse generate(YearMonth month) {
        LocalDate feeMonth = month.atDay(1);
        Map<Long, StudentAttendanceCount> counts = attendanceRepository.countByStudent(feeMonth, month.atEndOfMonth()).stream()
                .collect(Collectors.toMap(StudentAttendanceCount::studentId, Function.identity()));
        Map<Long, MonthlyFee> existing = feeRepository.findByMonthWithStudent(feeMonth).stream()
                .collect(Collectors.toMap(f -> f.getStudent().getId(), Function.identity()));
        Map<Long, Integer> scheduledByGroup = scheduleCalendar.classesScheduledByGroup(month);

        int created = 0;
        int updated = 0;
        List<MonthlyFee> toSave = new ArrayList<>();
        for (Student student : studentRepository.findAllActive()) {
            MonthlyFee fee = existing.get(student.getId());
            if (fee == null && student.getEnrollmentDate() != null && student.getEnrollmentDate().isAfter(month.atEndOfMonth())) {
                continue; // not enrolled yet in this month
            }
            if (fee == null) {
                fee = new MonthlyFee();
                fee.setStudent(student);
                fee.setFeeMonth(feeMonth);
                fee.setRate(Money.scale(student.getPerClassRate()));
                fee.setNotes(student.getClassGroup() == null ? null : student.getClassGroup().getName());
                created++;
            } else {
                updated++;
            }
            StudentAttendanceCount c = counts.get(student.getId());
            int held = c == null ? 0 : (int) c.held();
            fee.setClassesHeld(held);
            if (!fee.isScheduledOverride()) {
                Long groupId = student.getClassGroup() == null ? null : student.getClassGroup().getId();
                fee.setClassesScheduled(groupId == null ? null : scheduledByGroup.get(groupId));
            }
            fee.setAmountDue(calculator.amountDue(held, fee.getRate()));
            toSave.add(fee);
        }
        feeRepository.saveAll(toSave);
        log.info("Generated fees for {}: {} created, {} refreshed", month, created, updated);
        return new GenerateFeesResponse(month.toString(), created, updated, list(month, null, null));
    }

    @Transactional
    public FeeRowResponse update(Long feeId, UpdateMonthlyFeeRequest request) {
        MonthlyFee fee = requireFee(feeId);
        if (request.classesScheduled() != null) {
            fee.setClassesScheduled(request.classesScheduled());
            fee.setScheduledOverride(true);
        }
        fee.setNotes(request.notes() == null || request.notes().isBlank() ? null : request.notes().trim());
        if (request.rate() != null) {
            fee.setRate(Money.scale(request.rate()));
        }
        fee.setAmountDue(calculator.amountDue(fee.getClassesHeld(), fee.getRate()));
        return toRows(List.of(fee)).getFirst();
    }

    @Transactional
    public FeeRowResponse addPayment(Long feeId, CreatePaymentRequest request) {
        MonthlyFee fee = requireFee(feeId);
        FeePayment payment = new FeePayment();
        payment.setMonthlyFee(fee);
        apply(payment, request, fee);
        paymentRepository.save(payment);
        return toRows(List.of(fee)).getFirst();
    }

    @Transactional
    public FeeRowResponse updatePayment(Long feeId, Long paymentId, UpdatePaymentRequest request) {
        MonthlyFee fee = requireFee(feeId);
        FeePayment payment = paymentRepository.findByIdAndMonthlyFeeId(paymentId, feeId)
                .orElseThrow(() -> new NotFoundException("Payment", paymentId));
        apply(payment, request, fee);
        paymentRepository.flush();
        return toRows(List.of(fee)).getFirst();
    }

    @Transactional
    public FeeRowResponse deletePayment(Long feeId, Long paymentId) {
        MonthlyFee fee = requireFee(feeId);
        FeePayment payment = paymentRepository.findByIdAndMonthlyFeeId(paymentId, feeId)
                .orElseThrow(() -> new NotFoundException("Payment", paymentId));
        paymentRepository.delete(payment);
        paymentRepository.flush();
        return toRows(List.of(fee)).getFirst();
    }

    @Transactional(readOnly = true)
    public List<FeeRowResponse> studentHistory(Long studentId) {
        if (!studentRepository.existsById(studentId)) {
            throw new NotFoundException("Student", studentId);
        }
        List<MonthlyFee> fees = feeRepository.findByStudentIdOrderByFeeMonthDesc(studentId);
        if (fees.isEmpty()) {
            return List.of();
        }
        return toRows(feeRepository.findAllWithStudentByIdIn(fees.stream().map(MonthlyFee::getId).toList())).stream()
                .sorted(Comparator.comparing(FeeRowResponse::month).reversed())
                .toList();
    }

    /** Rows for the given fee ids (used by reminders). */
    @Transactional(readOnly = true)
    public List<FeeRowResponse> rows(Collection<Long> feeIds) {
        return toRows(feeRepository.findAllWithStudentByIdIn(feeIds));
    }

    @Transactional(readOnly = true)
    public List<FeeRowResponse> rowsForMonth(YearMonth month) {
        return toRows(feeRepository.findByMonthWithStudent(month.atDay(1)));
    }

    public FeeTotals totals(List<FeeRowResponse> rows) {
        BigDecimal due = rows.stream().map(FeeRowResponse::amountDue).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal paid = rows.stream().map(FeeRowResponse::amountPaid).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new FeeTotals(Money.scale(due), Money.scale(paid), calculator.balance(due, paid), rows.size());
    }

    private void apply(FeePayment payment, PaymentFields request, MonthlyFee fee) {
        LocalDate today = LocalDate.now(clock);
        if (request.paymentDate().isAfter(today)) {
            throw new BadRequestException("VALIDATION_FAILED", "cannot be in the future", "paymentDate");
        }
        if (request.paymentDate().isBefore(fee.getFeeMonth().minusYears(1))) {
            throw new BadRequestException("VALIDATION_FAILED",
                    "is more than a year before the fee month; check the date", "paymentDate");
        }
        payment.setAmount(Money.scale(request.amount()));
        payment.setPaymentDate(request.paymentDate());
        payment.setMethod(request.method());
        payment.setReference(request.reference() == null || request.reference().isBlank() ? null : request.reference().trim());
    }

    private MonthlyFee requireFee(Long id) {
        return feeRepository.findWithStudent(id).orElseThrow(() -> new NotFoundException("Monthly fee", id));
    }

    private List<FeeRowResponse> toRows(List<MonthlyFee> fees) {
        if (fees.isEmpty()) {
            return List.of();
        }
        Map<Long, List<FeePayment>> payments = new HashMap<>();
        for (FeePayment p : paymentRepository.findByFeeIds(fees.stream().map(MonthlyFee::getId).toList())) {
            payments.computeIfAbsent(p.getMonthlyFee().getId(), k -> new ArrayList<>()).add(p);
        }
        List<FeeRowResponse> rows = new ArrayList<>(fees.size());
        for (MonthlyFee f : fees) {
            List<FeePayment> ps = payments.getOrDefault(f.getId(), List.of());
            BigDecimal paid = Money.scale(ps.stream().map(FeePayment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
            FeePayment last = ps.stream()
                    .max(Comparator.comparing(FeePayment::getPaymentDate).thenComparing(FeePayment::getId))
                    .orElse(null);
            Student s = f.getStudent();
            List<PaymentResponse> paymentResponses = ps.stream().map(paymentMapper::toResponse).toList();
            rows.add(new FeeRowResponse(
                    f.getId(),
                    YearMonth.from(f.getFeeMonth()).toString(),
                    s.getId(),
                    s.getStudentCode(),
                    s.getFullName(),
                    s.getGrade(),
                    s.getClassGroup() == null ? null : s.getClassGroup().getName(),
                    s.getEmail(),
                    Money.scale(f.getRate()),
                    f.getClassesHeld(),
                    f.getClassesScheduled(),
                    f.isScheduledOverride(),
                    Money.scale(f.getAmountDue()),
                    paid,
                    calculator.balance(f.getAmountDue(), paid),
                    calculator.status(f.getAmountDue(), paid),
                    last == null ? null : last.getPaymentDate(),
                    last == null ? null : last.getMethod(),
                    f.getNotes(),
                    paymentResponses));
        }
        return rows;
    }
}
