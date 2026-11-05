package com.abkurapati.weconnect.user_service.common;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/** Domain exceptions plus a single place that maps them to RFC 7807 problem responses. */
public final class ApiExceptions {

    private ApiExceptions() {}

    public static class NotFoundException extends RuntimeException {
        public NotFoundException(String message) { super(message); }
    }

    public static class ConflictException extends RuntimeException {
        public ConflictException(String message) { super(message); }
    }

    public static class ForbiddenException extends RuntimeException {
        public ForbiddenException(String message) { super(message); }
    }

    public static class BadRequestException extends RuntimeException {
        public BadRequestException(String message) { super(message); }
    }

    @RestControllerAdvice
    public static class Handler {

        @ExceptionHandler(NotFoundException.class)
        ProblemDetail notFound(NotFoundException ex) { return problem(HttpStatus.NOT_FOUND, ex.getMessage()); }

        @ExceptionHandler({ConflictException.class, DataIntegrityViolationException.class})
        ProblemDetail conflict(RuntimeException ex) {
            return problem(HttpStatus.CONFLICT, ex instanceof ConflictException ? ex.getMessage() : "Resource already exists");
        }

        @ExceptionHandler(ForbiddenException.class)
        ProblemDetail forbidden(ForbiddenException ex) { return problem(HttpStatus.FORBIDDEN, ex.getMessage()); }

        @ExceptionHandler(BadRequestException.class)
        ProblemDetail badRequest(BadRequestException ex) { return problem(HttpStatus.BAD_REQUEST, ex.getMessage()); }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        ProblemDetail validation(MethodArgumentNotValidException ex) {
            ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, "Validation failed");
            Map<String, String> errors = new LinkedHashMap<>();
            ex.getBindingResult().getFieldErrors().forEach(e -> errors.putIfAbsent(e.getField(), e.getDefaultMessage()));
            pd.setProperty("errors", errors);
            return pd;
        }

        private static ProblemDetail problem(HttpStatus status, String detail) {
            return ProblemDetail.forStatusAndDetail(status, detail);
        }
    }
}
