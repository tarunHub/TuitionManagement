package com.smartstart.portal.student.service;

import com.smartstart.portal.attendance.repository.AttendanceRepository;
import com.smartstart.portal.classgroup.service.ClassGroupService;
import com.smartstart.portal.common.dto.PageResponse;
import com.smartstart.portal.common.exception.BadRequestException;
import com.smartstart.portal.common.exception.ConflictException;
import com.smartstart.portal.common.exception.NotFoundException;
import com.smartstart.portal.common.util.CsvWriter;
import com.smartstart.portal.common.util.Money;
import com.smartstart.portal.fee.repository.MonthlyFeeRepository;
import com.smartstart.portal.feedback.repository.MonthlyFeedbackRepository;
import com.smartstart.portal.graderate.entity.GradeRate;
import com.smartstart.portal.graderate.service.GradeRateService;
import com.smartstart.portal.student.dto.StudentDtos.CreateStudentRequest;
import com.smartstart.portal.student.dto.StudentDtos.StudentFields;
import com.smartstart.portal.student.dto.StudentDtos.StudentFilter;
import com.smartstart.portal.student.dto.StudentDtos.StudentResponse;
import com.smartstart.portal.student.dto.StudentDtos.UpdateStudentRequest;
import com.smartstart.portal.student.entity.Student;
import com.smartstart.portal.student.entity.StudentStatus;
import com.smartstart.portal.student.mapper.StudentMapper;
import com.smartstart.portal.student.repository.StudentRepository;
import com.smartstart.portal.student.repository.StudentSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentService {

    /** API sort keys → entity paths. Anything else is rejected. */
    private static final Map<String, String> SORTABLE = Map.ofEntries(
            Map.entry("studentCode", "studentCode"),
            Map.entry("fullName", "fullName"),
            Map.entry("grade", "grade"),
            Map.entry("classGroupName", "classGroup.name"),
            Map.entry("guardianName", "guardianName"),
            Map.entry("primaryContact", "primaryContact"),
            Map.entry("email", "email"),
            Map.entry("perClassRate", "perClassRate"),
            Map.entry("status", "status"),
            Map.entry("enrollmentDate", "enrollmentDate"),
            Map.entry("createdAt", "createdAt"));

    private final StudentRepository repository;
    private final StudentMapper mapper;
    private final GradeRateService gradeRateService;
    private final ClassGroupService classGroupService;
    private final AttendanceRepository attendanceRepository;
    private final MonthlyFeeRepository feeRepository;
    private final MonthlyFeedbackRepository feedbackRepository;
    private final BatchHistoryService batchHistory;

    @Transactional(readOnly = true)
    public PageResponse<StudentResponse> list(StudentFilter filter, Pageable pageable) {
        Pageable safe = PageRequest.of(pageable.getPageNumber(), Math.min(Math.max(pageable.getPageSize(), 1), 200),
                translateSort(pageable.getSort()));
        return PageResponse.of(repository.findAll(StudentSpecifications.matching(filter), safe), mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public StudentResponse get(Long id) {
        Student student = require(id);
        return mapper.toResponse(student).withHasHistory(hasHistory(id));
    }

    @Transactional
    public StudentResponse create(CreateStudentRequest request) {
        GradeRate grade = gradeRateService.requireGrade(request.grade());
        Student student = mapper.toEntity(request);
        applyCommon(student, request, grade);
        // Rule 3: new students default to the grade's rate unless an explicit override is supplied.
        student.setPerClassRate(Money.scale(request.perClassRate() != null ? request.perClassRate() : grade.getPerClassRate()));
        student.setStatus(request.status() == null ? StudentStatus.ACTIVE : request.status());
        // Rule 1: codes come from a forward-only sequence, so they are never reused.
        student.setStudentCode(formatCode(repository.nextCodeValue()));
        Student saved = repository.save(student);
        batchHistory.recordNew(saved);
        log.info("Created student {}", saved.getStudentCode());
        return mapper.toResponse(saved).withHasHistory(false);
    }

    @Transactional
    public StudentResponse update(Long id, UpdateStudentRequest request) {
        Student student = require(id);
        GradeRate grade = gradeRateService.requireGrade(request.grade());
        var previousGroup = student.getClassGroup();
        mapper.update(request, student);
        applyCommon(student, request, grade);
        student.setPerClassRate(Money.scale(request.perClassRate()));
        if (!BatchHistoryService.sameGroup(previousGroup, student.getClassGroup())) {
            // Only this month onwards moves; earlier months keep the old batch.
            batchHistory.recordMove(student);
        }
        return mapper.toResponse(student).withHasHistory(hasHistory(id));
    }

    @Transactional
    public StudentResponse updateStatus(Long id, StudentStatus status) {
        Student student = require(id);
        student.setStatus(status);
        return mapper.toResponse(student).withHasHistory(hasHistory(id));
    }

    /** Rule 2: hard delete only without history; otherwise the caller must deactivate instead. */
    @Transactional
    public void delete(Long id) {
        Student student = require(id);
        if (hasHistory(id)) {
            throw new ConflictException("STUDENT_HAS_HISTORY", "Student " + student.getStudentCode()
                    + " has attendance, fee or feedback history and cannot be deleted. Deactivate the student instead.");
        }
        repository.delete(student);
        log.info("Deleted student {}", student.getStudentCode());
    }

    @Transactional(readOnly = true)
    public String exportCsv(StudentFilter filter) {
        CsvWriter csv = new CsvWriter();
        csv.row(List.of("Student ID", "Full Name", "Date of Birth", "Grade", "School", "Subjects", "Group", "Guardian",
                "Primary Contact", "Alternate Contact", "Email", "Emergency Contact", "Emergency Phone",
                "Enrollment Date", "Rate", "Status", "Address", "Notes"));
        for (Student s : repository.findAll(StudentSpecifications.matching(filter), Sort.by("studentCode"))) {
            List<Object> row = new ArrayList<>();
            row.add(s.getStudentCode());
            row.add(s.getFullName());
            row.add(s.getDateOfBirth());
            row.add(s.getGrade());
            row.add(s.getSchool());
            row.add(String.join("; ", s.getSubjects()));
            row.add(s.getClassGroup() == null ? "" : s.getClassGroup().getName());
            row.add(s.getGuardianName());
            row.add(s.getPrimaryContact());
            row.add(s.getAlternateContact());
            row.add(s.getEmail());
            row.add(s.getEmergencyContactName());
            row.add(s.getEmergencyContactPhone());
            row.add(s.getEnrollmentDate());
            row.add(Money.scale(s.getPerClassRate()).toPlainString());
            row.add(s.getStatus());
            row.add(s.getAddress());
            row.add(s.getNotes());
            csv.row(row);
        }
        return csv.build();
    }

    @Transactional(readOnly = true)
    public Student require(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Student", id));
    }

    @Transactional(readOnly = true)
    public boolean hasHistory(Long studentId) {
        return attendanceRepository.existsByStudentId(studentId)
                || feeRepository.existsByStudentId(studentId)
                || feedbackRepository.existsByStudentId(studentId);
    }

    public static String formatCode(long sequenceValue) {
        return String.format(Locale.ROOT, "SS%03d", sequenceValue);
    }

    private void applyCommon(Student student, StudentFields request, GradeRate grade) {
        student.setGrade(grade.getGrade());
        student.setFullName(request.fullName().trim());
        String email = blankToNull(request.email());
        student.setEmail(email == null ? null : email.toLowerCase(Locale.ROOT));
        student.setPrimaryContact(blankToNull(request.primaryContact()));
        student.setAlternateContact(blankToNull(request.alternateContact()));
        student.setEmergencyContactName(blankToNull(request.emergencyContactName()));
        student.setEmergencyContactPhone(blankToNull(request.emergencyContactPhone()));
        student.setClassGroup(classGroupService.resolveOptional(request.classGroupId(), "classGroupId"));
        Set<String> subjects = new LinkedHashSet<>();
        if (request.subjects() != null) {
            request.subjects().stream().map(String::trim).filter(s -> !s.isEmpty()).forEach(subjects::add);
        }
        student.getSubjects().clear();
        student.getSubjects().addAll(subjects);
    }

    private static Sort translateSort(Sort sort) {
        if (sort.isUnsorted()) {
            return Sort.by("studentCode");
        }
        List<Sort.Order> orders = new ArrayList<>();
        for (Sort.Order order : sort) {
            String path = SORTABLE.get(order.getProperty());
            if (path == null) {
                throw new BadRequestException("INVALID_SORT", "Cannot sort by '" + order.getProperty() + "'");
            }
            orders.add(new Sort.Order(order.getDirection(), path).ignoreCase());
        }
        orders.add(Sort.Order.asc("studentCode"));
        return Sort.by(orders);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
