package com.policymesh.common;

import org.springframework.http.HttpStatus;

/** Domain error carrying the HTTP status and problem type slug it should surface as. */
public class ApiException extends RuntimeException {
  private final HttpStatus status;
  private final String type;

  public ApiException(HttpStatus status, String message) {
    this(status, slug(status), message);
  }

  public ApiException(HttpStatus status, String type, String message) {
    super(message);
    this.status = status;
    this.type = type;
  }

  public HttpStatus status() { return status; }
  public String type() { return type; }

  public static ApiException notFound(String message) { return new ApiException(HttpStatus.NOT_FOUND, message); }
  public static ApiException conflict(String message) { return new ApiException(HttpStatus.CONFLICT, message); }
  public static ApiException unprocessable(String message) { return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, message); }
  public static ApiException badRequest(String message) { return new ApiException(HttpStatus.BAD_REQUEST, message); }
  public static ApiException unauthorized(String message) { return new ApiException(HttpStatus.UNAUTHORIZED, message); }
  public static ApiException forbidden(String message) { return new ApiException(HttpStatus.FORBIDDEN, message); }
  public static ApiException unavailable(String message) { return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, message); }

  private static String slug(HttpStatus status) {
    return switch (status) {
      case BAD_REQUEST, UNPROCESSABLE_ENTITY -> "validation";
      case UNAUTHORIZED -> "unauthorized";
      case FORBIDDEN -> "forbidden";
      case NOT_FOUND -> "not-found";
      case CONFLICT -> "conflict";
      case SERVICE_UNAVAILABLE -> "service-unavailable";
      default -> "error";
    };
  }
}
