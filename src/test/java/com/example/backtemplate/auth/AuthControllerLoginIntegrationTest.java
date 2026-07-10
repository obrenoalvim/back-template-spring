package com.example.backtemplate.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.backtemplate.AbstractIntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

class AuthControllerLoginIntegrationTest extends AbstractIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void loginReturnsTokensForVerifiedUser() {
    restTemplate.postForEntity(
        "/auth/register",
        Map.of("email", "login@example.com", "password", "s3cret-pw"),
        Void.class);
    jdbcTemplate.update(
        "UPDATE users SET email_verified = true WHERE email = ?", "login@example.com");

    var resp =
        restTemplate.postForEntity(
            "/auth/login",
            Map.of("email", "login@example.com", "password", "s3cret-pw"),
            Map.class);

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp.getBody().get("accessToken")).isNotNull();
    assertThat(resp.getBody().get("refreshToken")).isNotNull();
  }

  @Test
  void loginRejectsUnverifiedUser() {
    restTemplate.postForEntity(
        "/auth/register",
        Map.of("email", "unverified@example.com", "password", "s3cret-pw"),
        Void.class);

    var resp =
        restTemplate.postForEntity(
            "/auth/login",
            Map.of("email", "unverified@example.com", "password", "s3cret-pw"),
            Map.class);

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }
}
