package com.smartstart.portal.feedback.service;

import com.smartstart.portal.common.exception.NotFoundException;
import com.smartstart.portal.feedback.dto.FeedbackDtos.CopyTopicsResult;
import com.smartstart.portal.feedback.dto.FeedbackDtos.FeedbackResponse;
import com.smartstart.portal.feedback.dto.FeedbackDtos.FeedbackRowResponse;
import com.smartstart.portal.feedback.dto.FeedbackDtos.UpsertFeedbackRequest;
import com.smartstart.portal.feedback.entity.MonthlyFeedback;
import com.smartstart.portal.feedback.mapper.FeedbackMapper;
import com.smartstart.portal.feedback.repository.MonthlyFeedbackRepository;
import com.smartstart.portal.student.entity.Student;
import com.smartstart.portal.student.repository.StudentRepository;
import com.smartstart.portal.student.service.MonthRoster;
import com.smartstart.portal.student.service.StudentService;
import com.smartstart.portal.common.util.MonthLock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final MonthlyFeedbackRepository repository;
    private final StudentRepository studentRepository;
    private final StudentService studentService;
    private final FeedbackMapper mapper;
    private final MonthRoster monthRoster;
    private final MonthLock monthLock;

    /**
     * Students for the month, with empty fields where feedback has not been entered. Past months also list
     * students deactivated since, and show each student in the batch they had that month.
     */
    @Transactional(readOnly = true)
    public List<FeedbackRowResponse> list(YearMonth month, Long groupId) {
        MonthRoster.Roster roster = monthRoster.forMonth(month, groupId);
        List<Student> students = roster.students();
        if (students.isEmpty()) {
            return List.of();
        }
        List<Long> ids = students.stream().map(Student::getId).toList();
        Map<Long, MonthlyFeedback> current = byStudent(repository.findForMonth(month.atDay(1), ids));
        Map<Long, MonthlyFeedback> previous = byStudent(repository.findForMonth(month.minusMonths(1).atDay(1), ids));
        return students.stream().map(s -> {
            MonthlyFeedback f = current.get(s.getId());
            MonthlyFeedback p = previous.get(s.getId());
            return new FeedbackRowResponse(
                    f == null ? null : f.getId(),
                    month.toString(),
                    s.getId(),
                    s.getStudentCode(),
                    s.getFullName(),
                    s.getGrade(),
                    roster.groupName(s),
                    s.getGuardianName(),
                    f == null ? null : f.getTopicsTaught(),
                    f == null ? null : f.getImprovementAreas(),
                    f == null ? null : f.getAchievements(),
                    p == null ? null : p.getTopicsTaught(),
                    f == null ? null : f.getUpdatedAt());
        }).toList();
    }

    /** Rule 8: one record per student per month, so PUT is an upsert. */
    @Transactional
    public FeedbackResponse upsert(Long studentId, YearMonth month, UpsertFeedbackRequest request) {
        monthLock.requireOpen(month, "feedback");
        Student student = studentService.require(studentId);
        LocalDate first = month.atDay(1);
        MonthlyFeedback feedback = repository.findByStudentIdAndFeedbackMonth(studentId, first).orElseGet(() -> {
            MonthlyFeedback f = new MonthlyFeedback();
            f.setStudent(student);
            f.setFeedbackMonth(first);
            return f;
        });
        feedback.setTopicsTaught(request.topicsTaught().trim());
        feedback.setImprovementAreas(trimToNull(request.improvementAreas()));
        feedback.setAchievements(trimToNull(request.achievements()));
        return mapper.toResponse(repository.saveAndFlush(feedback));
    }

    @Transactional
    public void delete(Long id) {
        MonthlyFeedback feedback = repository.findById(id).orElseThrow(() -> new NotFoundException("Feedback", id));
        monthLock.requireOpen(feedback.getFeedbackMonth(), "feedback");
        repository.delete(feedback);
    }

    /** Fills topics for active students without feedback this month from their previous month's topics. */
    @Transactional
    public CopyTopicsResult copyTopicsFromPreviousMonth(YearMonth month) {
        monthLock.requireOpen(month, "feedback");
        List<Student> students = studentRepository.findAllActive();
        if (students.isEmpty()) {
            return new CopyTopicsResult(0);
        }
        List<Long> ids = students.stream().map(Student::getId).toList();
        Map<Long, MonthlyFeedback> current = byStudent(repository.findForMonth(month.atDay(1), ids));
        Map<Long, MonthlyFeedback> previous = byStudent(repository.findForMonth(month.minusMonths(1).atDay(1), ids));
        int copied = 0;
        for (Student s : students) {
            MonthlyFeedback prev = previous.get(s.getId());
            if (current.containsKey(s.getId()) || prev == null || prev.getTopicsTaught() == null) {
                continue;
            }
            MonthlyFeedback f = new MonthlyFeedback();
            f.setStudent(s);
            f.setFeedbackMonth(month.atDay(1));
            f.setTopicsTaught(prev.getTopicsTaught());
            repository.save(f);
            copied++;
        }
        return new CopyTopicsResult(copied);
    }

    @Transactional(readOnly = true)
    public List<FeedbackResponse> studentHistory(Long studentId) {
        studentService.require(studentId);
        return repository.findByStudentIdOrderByFeedbackMonthDesc(studentId).stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public MonthlyFeedback require(Long studentId, YearMonth month) {
        return repository.findByStudentIdAndFeedbackMonth(studentId, month.atDay(1))
                .orElseThrow(() -> new NotFoundException("Feedback for student " + studentId + " in", month));
    }

    private static Map<Long, MonthlyFeedback> byStudent(List<MonthlyFeedback> list) {
        return list.stream().collect(Collectors.toMap(f -> f.getStudent().getId(), Function.identity()));
    }

    private static String trimToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
