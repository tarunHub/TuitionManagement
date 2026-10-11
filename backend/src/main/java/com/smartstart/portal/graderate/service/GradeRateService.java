package com.smartstart.portal.graderate.service;

import com.smartstart.portal.common.exception.ConflictException;
import com.smartstart.portal.common.exception.NotFoundException;
import com.smartstart.portal.common.util.Money;
import com.smartstart.portal.graderate.dto.GradeRateDtos.CreateGradeRateRequest;
import com.smartstart.portal.graderate.dto.GradeRateDtos.GradeRateResponse;
import com.smartstart.portal.graderate.dto.GradeRateDtos.StudentRef;
import com.smartstart.portal.graderate.dto.GradeRateDtos.UpdateGradeRateRequest;
import com.smartstart.portal.graderate.entity.GradeRate;
import com.smartstart.portal.graderate.mapper.GradeRateMapper;
import com.smartstart.portal.graderate.repository.GradeRateRepository;
import com.smartstart.portal.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class GradeRateService {

    private static final Pattern GRADE_NUMBER = Pattern.compile("(\\d+)");

    private final GradeRateRepository repository;
    private final StudentRepository studentRepository;
    private final GradeRateMapper mapper;

    @Transactional(readOnly = true)
    public List<GradeRateResponse> list() {
        Map<String, Long> counts = studentRepository.countByGrade();
        return repository.findAll(Sort.by("grade")).stream()
                .sorted(Comparator.comparingInt(GradeRateService::gradeOrder).thenComparing(GradeRate::getGrade))
                .map(g -> mapper.toResponse(g, counts.getOrDefault(g.getGrade().toLowerCase(), 0L)))
                .toList();
    }

    @Transactional
    public GradeRateResponse create(CreateGradeRateRequest request) {
        String grade = request.grade().trim();
        if (repository.existsByGradeIgnoreCase(grade)) {
            throw new ConflictException("DUPLICATE_GRADE", "Grade '" + grade + "' already exists");
        }
        GradeRate entity = mapper.toEntity(request);
        entity.setGrade(grade);
        entity.setPerClassRate(Money.scale(request.perClassRate()));
        return mapper.toResponse(repository.save(entity), 0);
    }

    /**
     * Updates a rate. Per business rule 3 this never touches existing students' rates or generated fees.
     */
    @Transactional
    public GradeRateResponse update(Long id, UpdateGradeRateRequest request) {
        GradeRate entity = find(id);
        String newGrade = request.grade().trim();
        if (!entity.getGrade().equalsIgnoreCase(newGrade)) {
            if (repository.existsByGradeIgnoreCase(newGrade)) {
                throw new ConflictException("DUPLICATE_GRADE", "Grade '" + newGrade + "' already exists");
            }
            List<StudentRef> users = studentRepository.findRefsByGrade(entity.getGrade());
            if (!users.isEmpty()) {
                throw new ConflictException("GRADE_IN_USE",
                        "Grade '" + entity.getGrade() + "' is used by " + users.size() + " student(s) and cannot be renamed", users);
            }
        }
        mapper.update(request, entity);
        entity.setGrade(newGrade);
        entity.setPerClassRate(Money.scale(request.perClassRate()));
        long count = studentRepository.findRefsByGrade(newGrade).size();
        return mapper.toResponse(entity, count);
    }

    @Transactional
    public void delete(Long id) {
        GradeRate entity = find(id);
        List<StudentRef> users = studentRepository.findRefsByGrade(entity.getGrade());
        if (!users.isEmpty()) {
            String names = users.stream().limit(10).map(s -> s.studentCode() + " " + s.fullName())
                    .reduce((a, b) -> a + ", " + b).orElse("");
            throw new ConflictException("GRADE_IN_USE",
                    "Grade '" + entity.getGrade() + "' is used by " + users.size() + " student(s): " + names, users);
        }
        repository.delete(entity);
    }

    @Transactional(readOnly = true)
    public GradeRate requireGrade(String grade) {
        return repository.findByGradeIgnoreCase(grade == null ? "" : grade.trim())
                .orElseThrow(() -> new com.smartstart.portal.common.exception.BadRequestException(
                        "VALIDATION_FAILED", "Grade '" + grade + "' does not exist in fee rates", "grade"));
    }

    private GradeRate find(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Grade rate", id));
    }

    /** Sorts SK/JK first, then Grade 1..12 numerically. */
    private static int gradeOrder(GradeRate g) {
        Matcher m = GRADE_NUMBER.matcher(g.getGrade());
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }
}
