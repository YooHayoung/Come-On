package com.comeon.authservice.web.common.exception;

import com.comeon.authservice.common.exception.CustomException;
import com.comeon.authservice.common.exception.ErrorCode;
import org.springframework.util.MultiValueMap;

public class ValidateException extends CustomException {

    private static final ErrorCode VALIDATION_FAIL = ErrorCode.VALIDATION_FAIL;

    private final MultiValueMap<String, String> errorResult;

    public ValidateException(String message, MultiValueMap<String, String> errorResult) {
        super(message, VALIDATION_FAIL);
        this.errorResult = errorResult;
    }

    public MultiValueMap<String, String> getErrorResult() {
        return errorResult;
    }
}

