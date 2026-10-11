package com.smartstart.portal.fee.repository;

import com.smartstart.portal.fee.entity.FeePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FeePaymentRepository extends JpaRepository<FeePayment, Long> {

    @Query("select p from FeePayment p where p.monthlyFee.id in :feeIds order by p.paymentDate, p.id")
    List<FeePayment> findByFeeIds(@Param("feeIds") Collection<Long> feeIds);

    Optional<FeePayment> findByIdAndMonthlyFeeId(Long id, Long monthlyFeeId);

    @Query("""
            select f.feeMonth, sum(p.amount) from FeePayment p join p.monthlyFee f
            where f.feeMonth between :from and :to group by f.feeMonth
            """)
    List<Object[]> sumPaidByFeeMonth(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("select f.student.id, sum(p.amount) from FeePayment p join p.monthlyFee f group by f.student.id")
    List<Object[]> sumPaidByStudent();
}
