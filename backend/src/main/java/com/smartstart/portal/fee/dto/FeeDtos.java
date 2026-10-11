package com.smartstart.portal.fee.dto;

import com.smartstart.portal.fee.entity.FeeStatus;
import com.smartstart.portal.fee.entity.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class FeeDtos {

    private FeeDtos() {
    }

    public record UpdateMonthlyFeeRequest(
            @Min(0) Integer classesScheduled,
            @Size(max = 2000) String notes,
            @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal rate
    ) {
    }

    public record CreatePaymentRequest(
            @NotNull @DecimalMin(value = "0.01", message = "must be greater than 0") @Digits(integer = 8, fraction = 2) BigDecimal amount,
            @NotNull @PastOrPresent(message = "cannot be in the future") LocalDate paymentDate,
            @NotNull PaymentMethod method,
            @Size(max = 100) String reference
    ) implements PaymentFields {
    }

    public record UpdatePaymentRequest(
            @NotNull @DecimalMin(value = "0.01", message = "must be greater than 0") @Digits(integer = 8, fraction = 2) BigDecimal amount,
            @NotNull @PastOrPresent(message = "cannot be in the future") LocalDate paymentDate,
            @NotNull PaymentMethod method,
            @Size(max = 100) String reference
    ) implements PaymentFields {
    }

    public interface PaymentFields {
        BigDecimal amount();

        LocalDate paymentDate();

        PaymentMethod method();

        String reference();
    }

    public record PaymentResponse(Long id, Long monthlyFeeId, BigDecimal amount, LocalDate paymentDate,
                                  PaymentMethod method, String reference, Instant createdAt) {
    }

    public record FeeRowResponse(
            Long id,
            String month,
            Long studentId,
            String studentCode,
            String fullName,
            String grade,
            String classGroupName,
            String studentEmail,
            BigDecimal rate,
            int classesHeld,
            Integer classesScheduled,
            boolean scheduledOverride,
            BigDecimal amountDue,
            BigDecimal amountPaid,
            BigDecimal balance,
            FeeStatus status,
            LocalDate lastPaymentDate,
            PaymentMethod lastPaymentMethod,
            String notes,
            List<PaymentResponse> payments
    ) {
    }

    public record FeeTotals(BigDecimal amountDue, BigDecimal amountPaid, BigDecimal balance, int count) {
    }

    public record FeeListResponse(String month, List<FeeRowResponse> rows, FeeTotals totals, int activeStudentsWithoutFee) {
    }

    public record GenerateFeesResponse(String month, int created, int updated, FeeListResponse fees) {
    }
}
