package com.example.demo.error;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * TURNS ERRORS INTO JSON ANSWERS — in one place, for every API.
 * ======================================================================================
 * When any layer throws, Spring stops the request and comes here instead of the controller's
 * normal return. The caller always gets the same shape:
 *
 *   { "error": { "code": "VALIDATION_FAILED", "message": "…", "details": { "name": "required" } } }
 *
 * Unexpected failures (a bug, the Data Store unreachable) become a plain 500 "Something went
 * wrong": the real reason goes to the server log, never to the browser.
 *
 * Real app: services/shared/.../error/ (ApiExceptionHandler and AppException).
 */
@RestControllerAdvice
public class ApiErrorHandler {

    private static final Logger LOG = Logger.getLogger(ApiErrorHandler.class.getName());

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> expected(ApiException e) {
        return ResponseEntity.status(e.getStatus()).body(body(e.getCode(), e.getMessage(), e.getFieldErrors()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> unexpected(Exception e) {
        LOG.log(Level.SEVERE, "Request failed", e);
        return ResponseEntity.status(500).body(body("INTERNAL_ERROR", "Something went wrong", null));
    }

    private static Map<String, Object> body(String code, String message, Map<String, String> details) {
        Map<String, Object> error = new LinkedHashMap<String, Object>();
        error.put("code", code);
        error.put("message", message);
        if (details != null) {
            error.put("details", details);
        }
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("error", error);
        return body;
    }
}
