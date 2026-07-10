package com.example.backtemplate.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.backtemplate.AbstractIntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

class AuthControllerResetPasswordIntegrationTest extends AbstractIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void forgotThenResetPasswordAllowsLoginWithNewPassword() {
    restTemplate.postForEntity(
        "/auth/register",
        Map.of("email", "reset@example.com", "password", "old-pw-123"),
        Void.class);
    jdbcTemplate.update(
        "UPDATE users SET email_verified = true WHERE email = ?", "reset@example.com");

    var forgotResp =
        restTemplate.postForEntity(
            "/auth/forgot-password", Map.of("email", "reset@example.com"), Void.class);
    assertThat(forgotResp.getStatusCode()).isEqualTo(HttpStatus.OK);

    String resetToken =
        jdbcTemplate.queryForObject(
            "SELECT reset_token FROM users WHERE email = ?", String.class, "reset@example.com");

    restTemplate.postForEntity(
        "/auth/reset-password",
        Map.of("token", resetToken, "newPassword", "new-pw-456"),
        Void.class);

    var loginResp =
        restTemplate.postForEntity(
            "/auth/login",
            Map.of("email", "reset@example.com", "password", "new-pw-456"),
            Map.class);
    assertThat(loginResp.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void forgotPasswordReturns200EvenForUnknownEmail() {
    var resp =
        restTemplate.postForEntity(
            "/auth/forgot-password", Map.of("email", "unknown@example.com"), Void.class);
    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
  }
}
