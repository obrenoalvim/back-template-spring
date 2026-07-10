package com.example.backtemplate.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ApiException extends RuntimeException {
  private final String code;
  private final HttpStatus status;

  public ApiException(String code, HttpStatus status, String message) {
    super(message);
    this.code = code;
    this.status = status;
  }

  public static ApiException notFound(String message) {
    return new ApiException("NOT_FOUND", HttpStatus.NOT_FOUND, message);
  }

  public static ApiException unauthorized(String message) {
    return new ApiException("UNAUTHORIZED", HttpStatus.UNAUTHORIZED, message);
  }

  public static ApiException conflict(String message) {
    return new ApiException("CONFLICT", HttpStatus.CONFLICT, message);
  }

  public static ApiException tooManyRequests(String message) {
    return new ApiException("RATE_LIMITED", HttpStatus.TOO_MANY_REQUESTS, message);
  }
}
