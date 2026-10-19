package com.smartstart.portal.student.service;

import com.smartstart.portal.classgroup.entity.ClassGroup;
import com.smartstart.portal.common.util.MonthLock;
import com.smartstart.portal.student.entity.Student;
import com.smartstart.portal.student.entity.StudentBatchHistory;
import com.smartstart.portal.student.repository.StudentBatchHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Remembers which batch each student was in, month by month, so moving a student between batches only changes
 * the current month onwards. Past months keep the batch (and so the class days and fees) they had.
 */
@Service
@RequiredArgsConstructor
public class BatchHistoryService {

    /** A new student's batch applies to every earlier month too. */
    static final LocalDate BEGINNING = LocalDate.of(1900, 1, 1);

    private final StudentBatchHistoryRepository repository;
    private final MonthLock monthLock;

    @Transactional
    public void recordNew(Student student) {
        repository.save(new StudentBatchHistory(student.getId(), BEGINNING, student.getClassGroup()));
    }

    /** Called after a student's batch changed: the new batch applies from the current month on. */
    @Transactional
    public void recordMove(Student student) {
        LocalDate month = monthLock.current().atDay(1);
        StudentBatchHistory row = repository.findById(new StudentBatchHistory.Key(student.getId(), month))
                .orElseGet(() -> new StudentBatchHistory(student.getId(), month, null));
        row.setClassGroup(student.getClassGroup());
        repository.save(row);
    }

    /** Student id → the batch they were in during the month (null = no batch). */
    @Transactional(readOnly = true)
    public Map<Long, ClassGroup> groupsIn(YearMonth month, Collection<Student> students) {
        Map<Long, ClassGroup> out = new HashMap<>();
        if (students.isEmpty()) {
            return out;
        }
        // Latest row first, so the first row seen per student is the one in force.
        for (StudentBatchHistory h : repository.findUpTo(students.stream().map(Student::getId).toList(), month.atDay(1))) {
            if (!out.containsKey(h.getId().studentId())) { // not putIfAbsent: a null batch is a real value here
                out.put(h.getId().studentId(), h.getClassGroup());
            }
        }
        // Students without history (should not happen after the migration) fall back to their current batch.
        for (Student s : students) {
            if (!out.containsKey(s.getId())) {
                out.put(s.getId(), s.getClassGroup());
            }
        }
        return out;
    }

    public static boolean sameGroup(ClassGroup a, ClassGroup b) {
        return Objects.equals(a == null ? null : a.getId(), b == null ? null : b.getId());
    }
}
