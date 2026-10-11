package com.smartstart.portal.student.repository;

import com.smartstart.portal.student.dto.StudentDtos.StudentFilter;
import com.smartstart.portal.student.entity.Student;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class StudentSpecifications {

    private StudentSpecifications() {
    }

    public static Specification<Student> matching(StudentFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter.search() != null && !filter.search().isBlank()) {
                String like = "%" + filter.search().trim().toLowerCase(Locale.ROOT)
                        .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("fullName")), like, '\\'),
                        cb.like(cb.lower(root.get("studentCode")), like, '\\'),
                        cb.like(cb.lower(root.get("guardianName")), like, '\\'),
                        cb.like(cb.lower(root.get("email")), like, '\\')));
            }
            if (filter.status() != null) {
                predicates.add(cb.equal(root.get("status"), filter.status()));
            }
            if (filter.grade() != null && !filter.grade().isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("grade")), filter.grade().trim().toLowerCase(Locale.ROOT)));
            }
            if (filter.groupId() != null) {
                predicates.add(cb.equal(root.get("classGroup").get("id"), filter.groupId()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
