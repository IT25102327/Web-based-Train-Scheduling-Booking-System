package com.trainbooking.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler providing centralized error handling across all controllers
 * with formatted JSON error responses and Thymeleaf fallback handling.
 *
 * @author SLIIT Software Engineering Team
 * @version 1.0.0
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles {@link ResourceNotFoundException} and returns HTTP 404 response.
     *
     * @param ex the exception thrown
     * @return JSON response with success=false and error message
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<Map<String, Object>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    /**
     * Handles custom {@link ApiException} and returns the corresponding HTTP status.
     *
     * @param ex the API exception thrown
     * @return JSON response with success=false and custom status code error
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> handleApiException(ApiException ex) {
        log.error("API Exception [Status {}]: {}", ex.getStatusCode(), ex.getMessage());
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("error", ex.getMessage());
        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }

    /**
     * Handles validation errors from {@link jakarta.validation.Valid} annotated parameters.
     *
     * @param ex the validation exception
     * @return HTTP 400 Bad Request with field error details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        log.warn("Validation failed: {}", ex.getMessage());
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("error", "Validation failed for one or more fields");
        body.put("errors", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Fallback handler for all uncaught generic exceptions.
     *
     * @param ex the unexpected exception
     * @return HTTP 500 Internal Server Error response
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Unhandled internal server exception occurred: ", ex);
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("error", "Internal Server Error");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
