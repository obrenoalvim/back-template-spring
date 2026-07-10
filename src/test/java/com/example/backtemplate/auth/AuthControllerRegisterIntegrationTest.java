package com.example.backtemplate.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.backtemplate.AbstractIntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

class AuthControllerRegisterIntegrationTest extends AbstractIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void registerCreatesUnverifiedUserAndVerifyActivatesIt() {
    var body = Map.of("email", "new@example.com", "password", "s3cret-pw");

    var registerResp = restTemplate.postForEntity("/auth/register", body, Void.class);
    assertThat(registerResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    Boolean verified =
        jdbcTemplate.queryForObject(
            "SELECT email_verified FROM users WHERE email = ?", Boolean.class, "new@example.com");
    assertThat(verified).isFalse();

    String token =
        jdbcTemplate.queryForObject(
            "SELECT verification_token FROM users WHERE email = ?",
            String.class,
            "new@example.com");

    restTemplate.getForEntity("/auth/verify-email?token=" + token, Void.class);

    verified =
        jdbcTemplate.queryForObject(
            "SELECT email_verified FROM users WHERE email = ?", Boolean.class, "new@example.com");
    assertThat(verified).isTrue();
  }
}
