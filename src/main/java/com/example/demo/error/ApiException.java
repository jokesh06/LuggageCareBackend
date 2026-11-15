package com.example.demo.error;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An error we MEAN to send back to the caller, with its HTTP status:
 *   401  not signed in            404  not found (or not yours)
 *   400  the input is wrong       (fieldErrors says which field and why)
 *
 * Any layer can `throw` one; ApiErrorHandler turns it into a JSON answer. Throwing stops the
 * request right there, so nothing after it runs (nothing half-saved).
 */
public class ApiException extends RuntimeException {

    private final int status;
    private final String code;
    private final Map<String, String> fieldErrors;

    public ApiException(int status, String code, String message, Map<String, String> fieldErrors) {
        super(message);
        this.status = status;
        this.code = code;
        this.fieldErrors = fieldErrors;
    }

    public static ApiException unauthenticated() {
        return new ApiException(401, "UNAUTHENTICATED", "Please sign in", null);
    }

    public static ApiException notFound(String what) {
        return new ApiException(404, "NOT_FOUND", what + " not found", null);
    }

    /** 400 with each wrong field, e.g. {"name": "required", "email": "invalid"}. */
    public static ApiException invalid(Map<String, String> fieldErrors) {
        return new ApiException(400, "VALIDATION_FAILED", "Some fields need attention",
                new LinkedHashMap<String, String>(fieldErrors));
    }

    public int getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
