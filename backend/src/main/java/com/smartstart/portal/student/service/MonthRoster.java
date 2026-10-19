package com.smartstart.portal.student.service;

import com.smartstart.portal.attendance.repository.AttendanceRepository;
import com.smartstart.portal.classgroup.entity.ClassGroup;
import com.smartstart.portal.common.util.MonthLock;
import com.smartstart.portal.fee.repository.MonthlyFeeRepository;
import com.smartstart.portal.feedback.repository.MonthlyFeedbackRepository;
import com.smartstart.portal.student.entity.Student;
import com.smartstart.portal.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The students to list for a month, each with the batch they were in that month. For past months this also
 * includes students who have since been deactivated, as long as they have attendance, fees or feedback that month.
 */
@Service
@RequiredArgsConstructor
public class MonthRoster {

    private final StudentRepository studentRepository;
    private final AttendanceRepository attendanceRepository;
    private final MonthlyFeeRepository feeRepository;
    private final MonthlyFeedbackRepository feedbackRepository;
    private final BatchHistoryService batchHistory;
    private final MonthLock monthLock;

    @Transactional(readOnly = true)
    public Roster forMonth(YearMonth month, Long groupId) {
        List<Student> candidates = new ArrayList<>(studentRepository.findAllActive());
        if (monthLock.isPast(month)) {
            Set<Long> ids = new HashSet<>(attendanceRepository.findStudentIdsBetween(month.atDay(1), month.atEndOfMonth()));
            ids.addAll(feeRepository.findStudentIdsForMonth(month.atDay(1)));
            ids.addAll(feedbackRepository.findStudentIdsForMonth(month.atDay(1)));
            candidates.forEach(s -> ids.remove(s.getId()));
            if (!ids.isEmpty()) {
                candidates.addAll(studentRepository.findAllWithGroupByIdIn(ids));
                candidates.sort(Comparator.comparing(Student::getStudentCode));
            }
        }
        Map<Long, ClassGroup> groups = batchHistory.groupsIn(month, candidates);
        List<Student> students = candidates.stream()
                .filter(s -> groupId == null || (groups.get(s.getId()) != null && groupId.equals(groups.get(s.getId()).getId())))
                .toList();
        return new Roster(students, groups);
    }

    public record Roster(List<Student> students, Map<Long, ClassGroup> groups) {

        public ClassGroup groupOf(Student s) {
            return groups.get(s.getId());
        }

        public String groupName(Student s) {
            ClassGroup g = groupOf(s);
            return g == null ? null : g.getName();
        }
    }
}
