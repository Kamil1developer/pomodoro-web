package com.pomodoro.app.exception;

import com.pomodoro.app.dto.ErrorResponse;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(AppException.class)
  public ResponseEntity<ErrorResponse> handleAppException(AppException ex) {
    return ResponseEntity.status(ex.getStatus())
        .body(new ErrorResponse("APP_ERROR", ex.getMessage(), List.of(), OffsetDateTime.now()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
    List<String> details =
        ex.getBindingResult().getFieldErrors().stream()
            .map(err -> err.getField() + ": " + err.getDefaultMessage())
            .toList();
    return ResponseEntity.badRequest()
        .body(
            new ErrorResponse(
                "VALIDATION_ERROR", "Validation failed", details, OffsetDateTime.now()));
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(new ErrorResponse("FORBIDDEN", "Access denied", List.of(), OffsetDateTime.now()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(
            new ErrorResponse("INTERNAL_ERROR", ex.getMessage(), List.of(), OffsetDateTime.now()));
  }
}
