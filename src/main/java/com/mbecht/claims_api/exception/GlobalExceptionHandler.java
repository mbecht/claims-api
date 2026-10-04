package com.mbecht.claims_api.exception;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Translates exceptions into RFC 9457 Problem Details responses.
 * Every response carries the standard status/title/detail fields plus
 * an {@code errorCode} and {@code timestamp} extension property.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // --- Our own domain exceptions ---

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleResourceNotFound(ResourceNotFoundException ex) {
        ProblemDetail problem = newProblemDetail(HttpStatus.NOT_FOUND, ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ProblemDetail> handleConflict(ConflictException ex) {
        ProblemDetail problem = newProblemDetail(HttpStatus.CONFLICT, ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ProblemDetail> handleBusinessRule(BusinessRuleException ex) {
        ProblemDetail problem = newProblemDetail(HttpStatus.UNPROCESSABLE_CONTENT, ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(problem);
    }

    // --- Anything not deliberately thrown by our own code ---

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        ProblemDetail problem = newProblemDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR, "An unexpected error occurred.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    // --- Spring MVC's built-in exceptions, routed through the same shape ---

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        List<FieldValidationError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldValidationError(fe.getField(), fe.getDefaultMessage()))
                .sorted(Comparator.comparing(FieldValidationError::field))
                .toList();

        ProblemDetail problem = newProblemDetail(
                HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, "One or more fields are invalid.");
        problem.setProperty("errors", errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        log.warn("Malformed request body: {}", ex.getMessage());

        ProblemDetail problem = newProblemDetail(
                HttpStatus.BAD_REQUEST, ErrorCode.MALFORMED_REQUEST, "The request body could not be read.");
        problem.setProperty("errors", List.of(new FieldValidationError("requestBody", "Malformed JSON request")));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        log.warn("Type mismatch for '{}': {}", ex.getPropertyName(), ex.getMessage());

        ProblemDetail problem = newProblemDetail(
                HttpStatus.BAD_REQUEST, ErrorCode.MALFORMED_REQUEST, "A request parameter has the wrong type.");
        problem.setProperty("errors", List.of(new FieldValidationError(ex.getPropertyName(), "Invalid value")));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    @Override
    protected ResponseEntity<Object> handleNoResourceFoundException(
            NoResourceFoundException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        ProblemDetail problem = newProblemDetail(
                HttpStatus.NOT_FOUND, ErrorCode.NO_SUCH_ENDPOINT, "No endpoint matches this URL.");

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    private ProblemDetail newProblemDetail(HttpStatus status, ErrorCode errorCode, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setProperty("errorCode", errorCode);
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    private record FieldValidationError(String field, String message) {
    }
}
