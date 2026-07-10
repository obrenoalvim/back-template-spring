package com.example.backtemplate.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  void mapsApiExceptionToErrorShape() {
    ApiException ex = ApiException.notFound("Note not found");

    ResponseEntity<ErrorResponse> response = handler.handleApiException(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody().error().code()).isEqualTo("NOT_FOUND");
    assertThat(response.getBody().error().message()).isEqualTo("Note not found");
  }
}
