package com.smartstart.portal.fee.service;

import com.smartstart.portal.common.util.Money;
import com.smartstart.portal.fee.entity.FeeStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Single home for money and percentage rules (sections 4.6, 4.7, 5 and 6.7). */
@Component
public class FeeCalculator {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    /** Rule 4: amount due = classes scheduled on the class calendar × rate. */
    public BigDecimal amountDue(Integer classesScheduled, BigDecimal rate) {
        int classes = classesScheduled == null ? 0 : Math.max(classesScheduled, 0);
        return Money.scale(Money.nz(rate).multiply(BigDecimal.valueOf(classes)));
    }

    public BigDecimal balance(BigDecimal amountDue, BigDecimal amountPaid) {
        return Money.scale(Money.nz(amountDue).subtract(Money.nz(amountPaid)));
    }

    /**
     * PAID when paid equals due (including a zero bill with no payments), CREDIT when overpaid,
     * UNPAID when nothing has been paid, otherwise PARTIAL.
     */
    public FeeStatus status(BigDecimal amountDue, BigDecimal amountPaid) {
        BigDecimal due = Money.scale(amountDue);
        BigDecimal paid = Money.scale(amountPaid);
        int cmp = paid.compareTo(due);
        if (cmp == 0) {
            return FeeStatus.PAID;
        }
        if (cmp > 0) {
            return FeeStatus.CREDIT;
        }
        return paid.signum() == 0 ? FeeStatus.UNPAID : FeeStatus.PARTIAL;
    }

    /** Collection rate as a percentage (e.g. 103.23); {@code null} when nothing was billed. */
    public BigDecimal collectionRatePct(BigDecimal billed, BigDecimal collected) {
        return percent(Money.nz(collected), Money.nz(billed));
    }

    /** Attendance % = present ÷ (present + absent); {@code null} when no classes were held. */
    public BigDecimal attendancePct(long present, long held) {
        return percent(BigDecimal.valueOf(present), BigDecimal.valueOf(held));
    }

    private static BigDecimal percent(BigDecimal part, BigDecimal whole) {
        if (whole.signum() == 0) {
            return null;
        }
        return part.multiply(HUNDRED).divide(whole, 2, RoundingMode.HALF_UP);
    }
}
