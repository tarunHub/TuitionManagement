package com.smartstart.portal.common.exception;

import org.springframework.http.HttpStatus;

public class FeatureNotConfiguredException extends ApiException {

    public FeatureNotConfiguredException(String message) {
        super(HttpStatus.NOT_IMPLEMENTED, "FEATURE_NOT_CONFIGURED", message);
    }
}
