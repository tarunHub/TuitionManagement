package com.smartstart.portal.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/** Base type for errors that map directly onto the API error shape. */
@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final String field;
    private final transient Object details;

    public ApiException(HttpStatus status, String code, String message) {
        this(status, code, message, null, null);
    }

    public ApiException(HttpStatus status, String code, String message, String field, Object details) {
        super(message);
        this.status = status;
        this.code = code;
        this.field = field;
        this.details = details;
    }
}
