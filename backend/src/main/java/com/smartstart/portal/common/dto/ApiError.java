package com.smartstart.portal.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        int status,
        String error,
        String message,
        List<FieldErrorDto> fieldErrors,
        Instant timestamp,
        String path,
        Object details
) {
    public record FieldErrorDto(String field, String message) {
    }
}
