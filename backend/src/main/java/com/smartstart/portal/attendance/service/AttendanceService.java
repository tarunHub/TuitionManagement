package com.smartstart.portal.attendance.service;

import com.smartstart.portal.calendar.service.ClassSessionService;
import com.smartstart.portal.attendance.dto.AttendanceDtos.AttendanceCell;
import com.smartstart.portal.attendance.dto.AttendanceDtos.AttendanceGridResponse;
import com.smartstart.portal.attendance.dto.AttendanceDtos.AttendanceHistoryEntry;
import com.smartstart.portal.attendance.dto.AttendanceDtos.AttendanceUpdate;
import com.smartstart.portal.attendance.dto.AttendanceDtos.BulkUpdateResult;
import com.smartstart.portal.attendance.dto.AttendanceDtos.GridDay;
import com.smartstart.portal.attendance.dto.AttendanceDtos.GridRow;
import com.smartstart.portal.attendance.dto.AttendanceDtos.GridTotals;
import com.smartstart.portal.attendance.dto.AttendanceDtos.MonthlyAttendanceSummary;
import com.smartstart.portal.attendance.dto.AttendanceDtos.StudentAttendanceHistory;
import com.smartstart.portal.attendance.entity.Attendance;
import com.smartstart.portal.attendance.entity.AttendanceStatus;
import com.smartstart.portal.attendance.repository.AttendanceRepository;
import com.smartstart.portal.common.exception.BadRequestException;
import com.smartstart.portal.common.exception.NotFoundException;
import com.smartstart.portal.common.util.AppTime;
import com.smartstart.portal.fee.service.FeeCalculator;
import com.smartstart.portal.student.entity.Student;
import com.smartstart.portal.classgroup.entity.ClassGroup;
import com.smartstart.portal.student.repository.StudentRepository;
import com.smartstart.portal.student.service.MonthRoster;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private static final int MAX_BULK = 5000;

    private final AttendanceRepository repository;
    private final StudentRepository studentRepository;
    private final FeeCalculator calculator;
    private final ClassSessionService classSessions;
    private final MonthRoster monthRoster;
    private final Clock clock;

    @Transactional(readOnly = true)
    public AttendanceGridResponse grid(YearMonth month, Long groupId) {
        LocalDate today = LocalDate.now(clock);
        LocalDate from = month.atDay(1);
        LocalDate to = month.atEndOfMonth();
        int length = month.lengthOfMonth();

        List<GridDay> days = new ArrayList<>(length);
        for (int d = 1; d <= length; d++) {
            LocalDate date = month.atDay(d);
            days.add(new GridDay(d, date, AppTime.dayLetter(date.getDayOfWeek()), AppTime.isWeekend(date), date.isAfter(today)));
        }

        // Students and batches as they were in that month, so later moves don't rewrite past months.
        MonthRoster.Roster roster = monthRoster.forMonth(month, groupId);
        List<Student> students = roster.students();
        Map<Long, Set<LocalDate>> classDays = classSessions.datesByGroup(month);
        Map<Long, AttendanceStatus[]> cellsByStudent = new HashMap<>();
        if (!students.isEmpty()) {
            Set<Long> ids = students.stream().map(Student::getId).collect(Collectors.toSet());
            for (AttendanceCell cell : repository.findCells(ids, from, to)) {
                cellsByStudent.computeIfAbsent(cell.studentId(), k -> new AttendanceStatus[length])[cell.date().getDayOfMonth() - 1] = cell.status();
            }
        }

        int[] presentPerDay = new int[length];
        int totalPresent = 0;
        int totalAbsent = 0;
        List<GridRow> rows = new ArrayList<>(students.size());
        for (Student s : students) {
            AttendanceStatus[] cells = cellsByStudent.getOrDefault(s.getId(), new AttendanceStatus[length]);
            int present = 0;
            int absent = 0;
            for (int i = 0; i < length; i++) {
                if (cells[i] == AttendanceStatus.PRESENT) {
                    present++;
                    presentPerDay[i]++;
                } else if (cells[i] == AttendanceStatus.ABSENT) {
                    absent++;
                }
            }
            totalPresent += present;
            totalAbsent += absent;
            rows.add(new GridRow(s.getId(), s.getStudentCode(), s.getFullName(),
                    roster.groupName(s),
                    Collections.unmodifiableList(java.util.Arrays.asList(cells)),
                    scheduledDays(s, roster.groupOf(s), month, classDays),
                    present, absent, present + absent, calculator.attendancePct(present, present + absent)));
        }

        List<Integer> perDay = java.util.Arrays.stream(presentPerDay).boxed().toList();
        int held = totalPresent + totalAbsent;
        return new AttendanceGridResponse(month.toString(), today, days, rows, perDay,
                new GridTotals(totalPresent, totalAbsent, held, calculator.attendancePct(totalPresent, held)));
    }

    /**
     * Applies a batch of cell changes atomically. Rule 7: only ACTIVE students and never for future dates.
     */
    @Transactional
    public BulkUpdateResult bulkUpdate(List<AttendanceUpdate> updates) {
        if (updates.isEmpty()) {
            return new BulkUpdateResult(0, 0, 0);
        }
        if (updates.size() > MAX_BULK) {
            throw new BadRequestException("TOO_MANY_CHANGES", "At most " + MAX_BULK + " changes can be saved at once");
        }
        LocalDate today = LocalDate.now(clock);

        // Last write wins for duplicate (student, date) pairs within one batch.
        Map<Key, AttendanceUpdate> byKey = new LinkedHashMap<>();
        for (AttendanceUpdate u : updates) {
            if (u.date().isAfter(today)) {
                throw new BadRequestException("FUTURE_DATE", "Attendance cannot be marked for a future date (" + u.date() + ")", "date");
            }
            byKey.put(new Key(u.studentId(), u.date()), u);
        }

        Set<Long> studentIds = byKey.keySet().stream().map(Key::studentId).collect(Collectors.toSet());
        Map<Long, Student> students = studentRepository.findAllById(studentIds).stream()
                .collect(Collectors.toMap(Student::getId, Function.identity()));
        for (Long id : studentIds) {
            Student s = students.get(id);
            if (s == null) {
                throw new NotFoundException("Student", id);
            }
            if (!s.isActive()) {
                throw new BadRequestException("STUDENT_INACTIVE",
                        "Attendance can only be marked for active students (" + s.getStudentCode() + " is inactive)", "studentId");
            }
        }

        LocalDate min = byKey.keySet().stream().map(Key::date).min(LocalDate::compareTo).orElseThrow();
        LocalDate max = byKey.keySet().stream().map(Key::date).max(LocalDate::compareTo).orElseThrow();
        Map<Key, Attendance> existing = repository.findForUpdate(studentIds, min, max).stream()
                .collect(Collectors.toMap(a -> new Key(a.getStudent().getId(), a.getClassDate()), Function.identity()));

        int created = 0;
        int updated = 0;
        int removed = 0;
        List<Attendance> toSave = new ArrayList<>();
        List<Attendance> toDelete = new ArrayList<>();
        for (Map.Entry<Key, AttendanceUpdate> e : byKey.entrySet()) {
            Attendance row = existing.get(e.getKey());
            AttendanceStatus status = e.getValue().status();
            if (status == null) {
                if (row != null) {
                    toDelete.add(row);
                    removed++;
                }
            } else if (row == null) {
                Attendance a = new Attendance();
                a.setStudent(students.get(e.getKey().studentId()));
                a.setClassDate(e.getKey().date());
                a.setStatus(status);
                toSave.add(a);
                created++;
            } else if (row.getStatus() != status) {
                row.setStatus(status);
                updated++;
            }
        }
        repository.deleteAll(toDelete);
        repository.flush();
        repository.saveAll(toSave);
        return new BulkUpdateResult(created, updated, removed);
    }

    @Transactional(readOnly = true)
    public StudentAttendanceHistory history(Long studentId) {
        if (!studentRepository.existsById(studentId)) {
            throw new NotFoundException("Student", studentId);
        }
        List<Attendance> rows = repository.findByStudentIdOrderByClassDateDesc(studentId);
        Map<YearMonth, int[]> byMonth = new LinkedHashMap<>();
        for (Attendance a : rows) {
            int[] counts = byMonth.computeIfAbsent(YearMonth.from(a.getClassDate()), k -> new int[2]);
            counts[a.getStatus() == AttendanceStatus.PRESENT ? 0 : 1]++;
        }
        List<MonthlyAttendanceSummary> months = byMonth.entrySet().stream()
                .map(e -> {
                    int p = e.getValue()[0];
                    int a = e.getValue()[1];
                    return new MonthlyAttendanceSummary(e.getKey().toString(), p, a, p + a, calculator.attendancePct(p, p + a));
                })
                .toList();
        List<AttendanceHistoryEntry> entries = rows.stream()
                .map(a -> new AttendanceHistoryEntry(a.getClassDate(), a.getStatus()))
                .toList();
        return new StudentAttendanceHistory(months, entries);
    }

    /** Class-calendar days of the student's batch, from the enrollment date on (a new student owes nothing earlier). */
    private static List<Boolean> scheduledDays(Student s, ClassGroup group, YearMonth month, Map<Long, Set<LocalDate>> classDays) {
        Set<LocalDate> days = group == null ? Set.of() : classDays.getOrDefault(group.getId(), Set.of());
        List<Boolean> out = new ArrayList<>(month.lengthOfMonth());
        for (int d = 1; d <= month.lengthOfMonth(); d++) {
            LocalDate date = month.atDay(d);
            out.add(days.contains(date) && (s.getEnrollmentDate() == null || !date.isBefore(s.getEnrollmentDate())));
        }
        return Collections.unmodifiableList(out);
    }

    private record Key(Long studentId, LocalDate date) {
    }
}
