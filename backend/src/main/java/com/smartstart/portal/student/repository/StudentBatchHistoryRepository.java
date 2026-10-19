package com.smartstart.portal.student.repository;

import com.smartstart.portal.student.entity.StudentBatchHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface StudentBatchHistoryRepository extends JpaRepository<StudentBatchHistory, StudentBatchHistory.Key> {

    /** Rows that started on or before the month, for the given students, latest first. */
    @Query("""
            select h from StudentBatchHistory h left join fetch h.classGroup
            where h.id.studentId in :studentIds and h.id.fromMonth <= :month
            order by h.id.fromMonth desc
            """)
    List<StudentBatchHistory> findUpTo(@Param("studentIds") Collection<Long> studentIds, @Param("month") LocalDate month);

    long countByClassGroupId(Long classGroupId);
}
