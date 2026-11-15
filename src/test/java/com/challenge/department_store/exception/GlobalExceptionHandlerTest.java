package com.challenge.department_store.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

class GlobalExceptionHandlerTest {

    private static final String REQUEST_PATH = "/api/inventories/15";

    private GlobalExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn(REQUEST_PATH);
        when(request.getMethod()).thenReturn("PUT");
        when(request.getContentType()).thenReturn("application/json");
    }

    @Test
    void testHandleNotFoundException() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleNotFound(new NotFoundException("Inventory not found"), request);

        assertResponse(response, HttpStatus.NOT_FOUND, "Inventory not found");
    }

    @Test
    void testHandleDuplicateEntityException() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleDuplicate(new DuplicateEntityException("Inventory already exists"), request);

        assertResponse(response, HttpStatus.CONFLICT, "Inventory already exists");
    }

    @Test
    void testHandleIllegalOperationException() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleIllegalOps(new IllegalOperationException("Invalid quantity"), request);

        assertResponse(response, HttpStatus.BAD_REQUEST, "Invalid quantity");
    }

    @Test
    void testHandleUnreadableRequestBody() {
        HttpMessageNotReadableException exception =
                mock(HttpMessageNotReadableException.class);

        ResponseEntity<Map<String, Object>> response =
                handler.handleUnreadableBody(exception, request);

        assertResponse(response, HttpStatus.BAD_REQUEST, "Malformed or invalid request body");
    }

    @Test
    void testHandlePathParameterTypeMismatch() {
        MethodArgumentTypeMismatchException exception =
                mock(MethodArgumentTypeMismatchException.class);
        when(exception.getName()).thenReturn("id");

        ResponseEntity<Map<String, Object>> response =
                handler.handleTypeMismatch(exception, request);

        assertResponse(response, HttpStatus.BAD_REQUEST, "Invalid value for path parameter: id");
    }

    @Test
    void testHandleInvalidRequestBodyField() {
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors())
                .thenReturn(List.of(new FieldError("request", "name", "must not be blank")));
        MethodArgumentNotValidException exception =
                mock(MethodArgumentNotValidException.class);
        when(exception.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<Map<String, Object>> response =
                handler.handleValidation(exception, request);

        assertResponse(response, HttpStatus.BAD_REQUEST, "name: must not be blank");
    }

    @Test
    void testHandleInvalidMethodParameter() {
        HandlerMethodValidationException exception =
                mock(HandlerMethodValidationException.class);

        ResponseEntity<Map<String, Object>> response =
                handler.handleMethodValidation(exception, request);

        assertResponse(response, HttpStatus.BAD_REQUEST, "Invalid request parameters");
    }

    @Test
    void testHandleDataIntegrityViolation() {
        DataIntegrityViolationException exception =
                new DataIntegrityViolationException("Constraint violation");

        ResponseEntity<Map<String, Object>> response =
                handler.handleDataIntegrity(exception, request);

        assertResponse(response, HttpStatus.CONFLICT, "The request conflicts with existing data");
    }

    @Test
    void testHandleUnexpectedExceptionWithoutExposingInternalMessage() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleUnexpected(new IllegalStateException("Internal detail"), request);

        assertResponse(response, HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    private void assertResponse(
            ResponseEntity<Map<String, Object>> response,
            HttpStatus expectedStatus,
            String expectedMessage) {
        assertThat(response.getStatusCode()).isEqualTo(expectedStatus);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody())
                .containsEntry("status", expectedStatus.value())
                .containsEntry("error", expectedStatus.getReasonPhrase())
                .containsEntry("message", expectedMessage)
                .containsEntry("path", REQUEST_PATH);
        assertThat(Instant.parse((String) response.getBody().get("timestamp"))).isNotNull();
    }
}
