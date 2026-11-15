package com.challenge.department_store.exception;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice 
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(
        NotFoundException exception, 
        HttpServletRequest req) {

        return build(HttpStatus.NOT_FOUND, exception.getMessage(), req);
    }

    @ExceptionHandler(DuplicateEntityException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicate(
        DuplicateEntityException exception, 
        HttpServletRequest req) {

        return build(HttpStatus.CONFLICT, exception.getMessage(), req);
    }

    @ExceptionHandler(IllegalOperationException.class)
    public ResponseEntity<Map<String, Object>> 
    handleIllegalOps(IllegalOperationException exception, 
        HttpServletRequest req) {

        return build(HttpStatus.BAD_REQUEST, exception.getMessage(), req);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadableBody(
        HttpMessageNotReadableException exception, 
        HttpServletRequest req) {
        
        return build(HttpStatus.BAD_REQUEST, "Malformed or invalid request body", req);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(
        MethodArgumentTypeMismatchException exception, 
        HttpServletRequest req) {
        
        return build(HttpStatus.BAD_REQUEST, "Invalid value for path parameter: " + exception.getName(), req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest req) {
                
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("Invalid request");

        return build(HttpStatus.BAD_REQUEST, message, req);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<Map<String, Object>> handleMethodValidation(
            HandlerMethodValidationException exception,
            HttpServletRequest req) {

        return build(HttpStatus.BAD_REQUEST, "Invalid request parameters", req);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(
        DataIntegrityViolationException exception, 
        HttpServletRequest req) {

        return build(HttpStatus.CONFLICT, "The request conflicts with existing data", req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(
        Exception exception, 
        HttpServletRequest req) {

        logger.error("Unexpected error processing request", exception);
        logger.debug("Received request: method={} uri={} contentType={}",
            req.getMethod(),
            req.getRequestURI(),
            req.getContentType()
);

        return build(HttpStatus.INTERNAL_SERVER_ERROR,"An unexpected error occurred", req);
    }

    private ResponseEntity<Map<String, Object>> build(
        HttpStatus status, String message, 
        HttpServletRequest req) {

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", req.getRequestURI());

        return ResponseEntity.status(status).body(body);
    }
}
