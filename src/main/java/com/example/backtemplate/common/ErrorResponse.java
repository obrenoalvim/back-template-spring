package com.example.backtemplate.common;

import java.util.List;

public record ErrorResponse(ErrorBody error) {
  public record ErrorBody(String code, String message, List<String> details) {}

  public static ErrorResponse of(String code, String message) {
    return new ErrorResponse(new ErrorBody(code, message, List.of()));
  }

  public static ErrorResponse of(String code, String message, List<String> details) {
    return new ErrorResponse(new ErrorBody(code, message, details));
  }
}
