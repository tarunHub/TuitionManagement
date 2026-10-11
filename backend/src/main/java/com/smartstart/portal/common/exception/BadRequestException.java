package com.smartstart.portal.common.exception;

import org.springframework.http.HttpStatus;

public class BadRequestException extends ApiException {

    public BadRequestException(String code, String message) {
        super(HttpStatus.BAD_REQUEST, code, message);
    }

    /** A validation failure attributed to a single request field. */
    public BadRequestException(String code, String message, String field) {
        super(HttpStatus.BAD_REQUEST, code, message, field, null);
    }
}
