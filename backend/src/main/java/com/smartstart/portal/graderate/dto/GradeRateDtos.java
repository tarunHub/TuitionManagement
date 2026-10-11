package com.smartstart.portal.graderate.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public final class GradeRateDtos {

    private GradeRateDtos() {
    }

    public record CreateGradeRateRequest(
            @NotBlank @Size(max = 30) String grade,
            @NotNull @DecimalMin(value = "0.01", message = "must be greater than 0") @Digits(integer = 8, fraction = 2) BigDecimal perClassRate,
            @Size(max = 2000) String notes
    ) {
    }

    public record UpdateGradeRateRequest(
            @NotBlank @Size(max = 30) String grade,
            @NotNull @DecimalMin(value = "0.01", message = "must be greater than 0") @Digits(integer = 8, fraction = 2) BigDecimal perClassRate,
            @Size(max = 2000) String notes
    ) {
    }

    public record GradeRateResponse(
            Long id,
            String grade,
            BigDecimal perClassRate,
            String notes,
            long studentCount,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record StudentRef(Long id, String studentCode, String fullName) {
    }
}
