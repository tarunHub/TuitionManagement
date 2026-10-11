package com.smartstart.portal.common.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends ApiException {

    public ConflictException(String code, String message) {
        super(HttpStatus.CONFLICT, code, message);
    }

    public ConflictException(String code, String message, Object details) {
        super(HttpStatus.CONFLICT, code, message, null, details);
    }
}
