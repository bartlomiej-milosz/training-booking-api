package dev.bartmilo.trainingbooking.shared;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {
  @ExceptionHandler(ApiException.class)
  ProblemDetail domain(ApiException exception, HttpServletRequest request) {
    return problem(exception.status(), exception.code(), exception.getMessage(), request);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ProblemDetail validation(MethodArgumentNotValidException exception, HttpServletRequest request) {
    var detail =
        problem(
            HttpStatus.BAD_REQUEST,
            "VALIDATION_ERROR",
            "The request contains invalid fields.",
            request);
    detail.setProperty(
        "errors",
        exception.getBindingResult().getFieldErrors().stream()
            .map(
                error ->
                    Map.of(
                        "field",
                        error.getField(),
                        "message",
                        error.getDefaultMessage() == null
                            ? "Invalid value"
                            : error.getDefaultMessage()))
            .toList());
    return detail;
  }

  @ExceptionHandler(HandlerMethodValidationException.class)
  ProblemDetail parameters(HandlerMethodValidationException exception, HttpServletRequest request) {
    var detail =
        problem(
            HttpStatus.BAD_REQUEST,
            "VALIDATION_ERROR",
            "The request contains invalid parameters.",
            request);
    detail.setProperty(
        "errors",
        exception.getParameterValidationResults().stream()
            .flatMap(
                result ->
                    result.getResolvableErrors().stream()
                        .map(
                            error ->
                                Map.of(
                                    "field",
                                        result.getMethodParameter().getParameterName() == null
                                            ? "request"
                                            : result.getMethodParameter().getParameterName(),
                                    "message",
                                        error.getDefaultMessage() == null
                                            ? "Invalid value"
                                            : error.getDefaultMessage())))
            .toList());
    return detail;
  }

  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class
  })
  ProblemDetail malformed(Exception exception, HttpServletRequest request) {
    return problem(
        HttpStatus.BAD_REQUEST,
        "INVALID_REQUEST",
        "The request body or parameter format is invalid.",
        request);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ProblemDetail conflict(DataIntegrityViolationException exception, HttpServletRequest request) {
    Throwable cause = exception;
    while (cause != null) {
      if (cause instanceof org.hibernate.exception.ConstraintViolationException violation
          && "uk_room_name".equals(violation.getConstraintName())) {
        return problem(
            HttpStatus.CONFLICT,
            "ROOM_NAME_ALREADY_EXISTS",
            "A room with this name already exists.",
            request);
      }
      cause = cause.getCause();
    }
    return problem(
        HttpStatus.CONFLICT,
        "DATA_CONFLICT",
        "The operation conflicts with existing data.",
        request);
  }

  @ExceptionHandler(AccessDeniedException.class)
  ProblemDetail forbidden(AccessDeniedException exception, HttpServletRequest request) {
    return problem(
        HttpStatus.FORBIDDEN,
        "FORBIDDEN",
        "You do not have permission to perform this operation.",
        request);
  }

  private static ProblemDetail problem(
      HttpStatus status, String code, String message, HttpServletRequest request) {
    var detail = ProblemDetail.forStatusAndDetail(status, message);
    detail.setInstance(URI.create(request.getRequestURI()));
    detail.setProperty("code", code);
    return detail;
  }
}
