package com.smartstart.portal.fee.entity;

import com.smartstart.portal.common.entity.BaseEntity;
import com.smartstart.portal.student.entity.Student;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "monthly_fee")
public class MonthlyFee extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    /** First day of the billed month. */
    @Column(name = "fee_month", nullable = false)
    private LocalDate feeMonth;

    /** Snapshot of the student's per-class rate when the fee was first generated. */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal rate;

    @Column(name = "classes_held", nullable = false)
    private int classesHeld;

    @Column(name = "classes_scheduled")
    private Integer classesScheduled;

    /** True once an admin entered classes_scheduled by hand; regeneration then leaves it alone. */
    @Column(name = "scheduled_override", nullable = false)
    private boolean scheduledOverride;

    @Column(name = "amount_due", nullable = false, precision = 10, scale = 2)
    private BigDecimal amountDue = BigDecimal.ZERO;

    @Column(columnDefinition = "text")
    private String notes;
}
