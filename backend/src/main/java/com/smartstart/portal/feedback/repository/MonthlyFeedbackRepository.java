package com.smartstart.portal.feedback.repository;

import com.smartstart.portal.feedback.entity.MonthlyFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MonthlyFeedbackRepository extends JpaRepository<MonthlyFeedback, Long> {

    boolean existsByStudentId(Long studentId);

    @Query("select f from MonthlyFeedback f where f.feedbackMonth = :month and f.student.id in :studentIds")
    List<MonthlyFeedback> findForMonth(@Param("month") LocalDate month, @Param("studentIds") Collection<Long> studentIds);

    Optional<MonthlyFeedback> findByStudentIdAndFeedbackMonth(Long studentId, LocalDate feedbackMonth);

    List<MonthlyFeedback> findByStudentIdOrderByFeedbackMonthDesc(Long studentId);

    @Query("select f.student.id from MonthlyFeedback f where f.feedbackMonth = :month")
    List<Long> findStudentIdsForMonth(@Param("month") LocalDate month);

    @Query("""
            select count(s) from Student s
            where s.status = com.smartstart.portal.student.entity.StudentStatus.ACTIVE
              and not exists (select 1 from MonthlyFeedback f where f.student = s and f.feedbackMonth = :month)
            """)
    long countActiveWithoutFeedback(@Param("month") LocalDate month);
}
