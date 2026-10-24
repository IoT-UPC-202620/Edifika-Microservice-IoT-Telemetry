package com.edifika.telemetry.telemetry.interfaces.rest.exceptions;

import com.edifika.telemetry.telemetry.interfaces.rest.resources.ErrorResource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.NoSuchElementException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResource> handleNotFound(NoSuchElementException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResource> handleBadRequest(IllegalArgumentException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResource> handleMalformedRequest(Exception ex) {
        return build(HttpStatus.BAD_REQUEST, "La solicitud tiene un formato inválido");
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResource> handleConflict(IllegalStateException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResource> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.warn("Violación de integridad de datos: {}", ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT, "La solicitud entra en conflicto con un registro existente");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResource> handleUnauthorized(AuthenticationException ex) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResource> handleForbidden(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, "No tiene permisos para realizar esta operación");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResource> handleUnexpected(Exception ex) {
        log.error("Error no controlado", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado, intente nuevamente");
    }

    private static ResponseEntity<ErrorResource> build(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ErrorResource.of(status, message));
    }
}
