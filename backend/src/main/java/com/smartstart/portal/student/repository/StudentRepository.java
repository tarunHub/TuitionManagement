package com.smartstart.portal.student.repository;

import com.smartstart.portal.graderate.dto.GradeRateDtos.StudentRef;
import com.smartstart.portal.student.entity.Student;
import com.smartstart.portal.student.entity.StudentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface StudentRepository extends JpaRepository<Student, Long>, JpaSpecificationExecutor<Student> {

    @Query(value = "SELECT nextval('student_code_seq')", nativeQuery = true)
    long nextCodeValue();

    @Override
    @EntityGraph(attributePaths = "classGroup")
    Page<Student> findAll(Specification<Student> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "classGroup")
    List<Student> findAll(Specification<Student> spec, org.springframework.data.domain.Sort sort);

    /** Active students (optionally in one group), ordered by code, with their group fetched. */
    @Query("""
            select s from Student s left join fetch s.classGroup g
            where s.status = com.smartstart.portal.student.entity.StudentStatus.ACTIVE
              and (:groupId is null or g.id = :groupId)
            order by s.studentCode
            """)
    List<Student> findActive(@Param("groupId") Long groupId);

    @Query("""
            select s from Student s left join fetch s.classGroup
            where s.status = com.smartstart.portal.student.entity.StudentStatus.ACTIVE
            order by s.studentCode
            """)
    List<Student> findAllActive();

    @Query("""
            select s from Student s left join fetch s.classGroup g
            where s.status = com.smartstart.portal.student.entity.StudentStatus.ACTIVE and g.id in :groupIds
            order by s.studentCode
            """)
    List<Student> findActiveInGroups(@Param("groupIds") Collection<Long> groupIds);

    @Query("select s from Student s left join fetch s.classGroup where s.id in :ids order by s.studentCode")
    List<Student> findAllWithGroupByIdIn(@Param("ids") Collection<Long> ids);

    long countByStatus(StudentStatus status);

    long countByClassGroupId(Long classGroupId);

    @Query("""
            select new com.smartstart.portal.graderate.dto.GradeRateDtos$StudentRef(s.id, s.studentCode, s.fullName)
            from Student s where lower(s.grade) = lower(:grade) order by s.studentCode
            """)
    List<StudentRef> findRefsByGrade(@Param("grade") String grade);

    @Query("select lower(s.grade), count(s) from Student s group by lower(s.grade)")
    List<Object[]> countByGradeRaw();

    default Map<String, Long> countByGrade() {
        Map<String, Long> out = new HashMap<>();
        countByGradeRaw().forEach(r -> out.put((String) r[0], (Long) r[1]));
        return out;
    }

    @Query("""
            select s.classGroup.id, count(s) from Student s
            where s.status = com.smartstart.portal.student.entity.StudentStatus.ACTIVE and s.classGroup is not null
            group by s.classGroup.id
            """)
    List<Object[]> countActiveByGroupRaw();

    default Map<Long, Long> countActiveByGroup() {
        Map<Long, Long> out = new HashMap<>();
        countActiveByGroupRaw().forEach(r -> out.put((Long) r[0], (Long) r[1]));
        return out;
    }
}
