package com.hotel.controllers.advice;


import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(ApiExceptionHandler.class);

    /**
     * UNAUTHORIZED (401)
     * Para errores de autenticación: credenciales inválidas, token expirado, etc.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuth(AuthenticationException ex) {
        return build(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Credenciales inválidas.");
    }


    /**
     * NOT FOUND (404)
     * Para recursos que no existen: Unidad, Habitacion, Ocupante, Reserva, etc.
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(EntityNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage());
    }

    /**
     * BAD REQUEST (400)
     * Para parámetros inválidos o datos incoherentes (no es regla de negocio, es input incorrecto).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage());
    }

    /**
     * CONFLICT (409)
     * Para reglas de negocio: no disponible, ya ocupado, estado inválido para la operación, etc.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(IllegalStateException ex) {
        return build(HttpStatus.CONFLICT, "CONFLICT", ex.getMessage());
    }

    /**
     * VALIDATION (400)
     * Para errores de @Valid en DTOs.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(err ->
                fieldErrors.put(err.getField(), err.getDefaultMessage())
        );

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", OffsetDateTime.now().toString());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "VALIDATION_ERROR");
        body.put("message", "El request contiene errores de validación.");
        body.put("fields", fieldErrors);

        return ResponseEntity.badRequest().body(body);
    }

    /**
     * BAD REQUEST (400)
     * Para query params requeridos que no fueron enviados.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingRequestParam(MissingServletRequestParameterException ex) {
        String message = "El parámetro '" + ex.getParameterName() + "' es obligatorio.";
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message);
    }

    /**
     * BAD REQUEST (400)
     * Para errores de tipo/formato en path/query params (enum, fechas, números, etc.).
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String tipoEsperado = ex.getRequiredType() != null
                ? ex.getRequiredType().getSimpleName()
                : "tipo válido";
        String message = "El parámetro '" + ex.getName() + "' tiene un valor inválido. Se esperaba "
                + tipoEsperado + ".";
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message);
    }

    /**
     * BAD REQUEST (400)
     * Para errores de parseo/binding en query/form params.
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Map<String, Object>> handleBindException(BindException ex) {
        String message = "Uno o más parámetros del request tienen un formato inválido.";
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message);
    }

    /**
     * BAD REQUEST (400)
     * Para errores de parseo del body (JSON mal formado, fechas/enums inválidos, etc.).
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleNotReadable(HttpMessageNotReadableException ex) {
        String message = "El cuerpo del request es inválido o tiene tipos de dato incorrectos.";
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message);
    }

    /**
     * BAD REQUEST (400)
     * Para validaciones por constraints en parámetros.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex) {
        String message = "El request contiene parámetros inválidos.";
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message);
    }

    /**
     * FALLBACK (500)
     * Para cualquier error no contemplado.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        logger.error("Error inesperado", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR",
                "Ocurrió un error inesperado.");
    }


    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String error, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", OffsetDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", message);

        return ResponseEntity.status(status).body(body);
    }
}
