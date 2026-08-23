package com.policymesh.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.util.List;
import java.util.Map;

/** Maps every failure to RFC 7807 application/problem+json without leaking internals. */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(ApiException.class)
  public ProblemDetail api(ApiException ex, HttpServletRequest req) {
    return problem(ex.status(), ex.type(), title(ex.status()), ex.getMessage(), req, null);
  }

  @ExceptionHandler(com.policymesh.ci.git.CiValidationException.class)
  public ProblemDetail ciValidation(com.policymesh.ci.git.CiValidationException ex, HttpServletRequest req) {
    Map<String, Object> props = new java.util.HashMap<>();
    props.put("errorCode", ex.getErrorCode());
    if (ex.getBranch() != null) props.put("branch", ex.getBranch());
    if (ex.getCommitHash() != null) props.put("commitHash", ex.getCommitHash());
    return problem(HttpStatus.UNPROCESSABLE_ENTITY, "ci-validation", "Invalid Commit Reference", ex.getMessage(), req, props);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ProblemDetail validation(MethodArgumentNotValidException ex, HttpServletRequest req) {
    List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
        .map(e -> Map.of("field", e.getField(), "message", String.valueOf(e.getDefaultMessage())))
        .toList();
    String detail = errors.isEmpty() ? "Validation failed" : errors.getFirst().get("field") + ": " + errors.getFirst().get("message");
    return problem(HttpStatus.UNPROCESSABLE_ENTITY, "validation", "Validation failed", detail, req, Map.of("errors", errors));
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ProblemDetail constraint(ConstraintViolationException ex, HttpServletRequest req) {
    return problem(HttpStatus.UNPROCESSABLE_ENTITY, "validation", "Validation failed", ex.getMessage(), req, null);
  }

  @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class, IllegalArgumentException.class})
  public ProblemDetail malformed(Exception ex, HttpServletRequest req) {
    return problem(HttpStatus.BAD_REQUEST, "malformed-request", "Malformed request", "The request body or parameters could not be parsed", req, null);
  }

  @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
  public ProblemDetail noHandler(Exception ex, HttpServletRequest req) {
    return problem(HttpStatus.NOT_FOUND, "not-found", "Not Found", "No such endpoint: " + req.getRequestURI(), req, null);
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ProblemDetail mediaType(HttpMediaTypeNotSupportedException ex, HttpServletRequest req) {
    return problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "unsupported-media-type", "Unsupported Media Type", ex.getMessage(), req, null);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ProblemDetail integrity(DataIntegrityViolationException ex, HttpServletRequest req) {
    return problem(HttpStatus.CONFLICT, "conflict", "Conflict", "The request conflicts with existing data", req, null);
  }

  @ExceptionHandler(Exception.class)
  public ProblemDetail unknown(Exception ex, HttpServletRequest req) {
    log.error("Unhandled exception on {} {}", req.getMethod(), req.getRequestURI(), ex);
    return problem(HttpStatus.INTERNAL_SERVER_ERROR, "internal", "Internal Server Error", "An unexpected server error occurred", req, null);
  }

  static ProblemDetail problem(HttpStatus status, String type, String title, String detail, HttpServletRequest req, Map<String, Object> properties) {
    ProblemDetail p = ProblemDetail.forStatusAndDetail(status, detail == null ? title : detail);
    p.setTitle(title);
    p.setType(URI.create("https://policymesh/errors/" + type));
    p.setInstance(URI.create(req.getRequestURI()));
    if (properties != null) properties.forEach(p::setProperty);
    return p;
  }

  private static String title(HttpStatus status) {
    return switch (status) {
      case BAD_REQUEST, UNPROCESSABLE_ENTITY -> "Validation failed";
      default -> status.getReasonPhrase() != null ? status.getReasonPhrase() : "Error";
    };
  }
}
