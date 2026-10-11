package com.smartstart.portal.fee.repository;

import com.smartstart.portal.fee.entity.MonthlyFee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MonthlyFeeRepository extends JpaRepository<MonthlyFee, Long> {

    boolean existsByStudentId(Long studentId);

    @Query("""
            select f from MonthlyFee f join fetch f.student s left join fetch s.classGroup
            where f.feeMonth = :month order by s.studentCode
            """)
    List<MonthlyFee> findByMonthWithStudent(@Param("month") LocalDate month);

    @Query("""
            select f from MonthlyFee f join fetch f.student s left join fetch s.classGroup
            where f.id in :ids order by s.studentCode
            """)
    List<MonthlyFee> findAllWithStudentByIdIn(@Param("ids") Collection<Long> ids);

    @Query("select f from MonthlyFee f join fetch f.student s left join fetch s.classGroup where f.id = :id")
    Optional<MonthlyFee> findWithStudent(@Param("id") Long id);

    List<MonthlyFee> findByStudentIdOrderByFeeMonthDesc(Long studentId);

    @Query("select f.feeMonth, sum(f.amountDue) from MonthlyFee f where f.feeMonth between :from and :to group by f.feeMonth")
    List<Object[]> sumDueByMonth(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("select f.student.id, sum(f.amountDue) from MonthlyFee f group by f.student.id")
    List<Object[]> sumDueByStudent();
}
