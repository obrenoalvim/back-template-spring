package com.example.backtemplate.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.backtemplate.AbstractIntegrationTest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;

class AdminControllerIntegrationTest extends AbstractIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private JdbcTemplate jdbcTemplate;

  private String login(String email, String password) {
    restTemplate.postForEntity(
        "/auth/register", Map.of("email", email, "password", password), Void.class);
    jdbcTemplate.update("UPDATE users SET email_verified = true WHERE email = ?", email);
    var resp =
        restTemplate.postForEntity(
            "/auth/login", Map.of("email", email, "password", password), Map.class);
    return (String) resp.getBody().get("accessToken");
  }

  @Test
  void regularUserIsForbiddenFromListingUsers() {
    String token = login("regular@example.com", "password-123");
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);

    var resp =
        restTemplate.exchange(
            "/admin/users", HttpMethod.GET, new HttpEntity<>(headers), Void.class);

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void adminCanListUsers() {
    String token = login("admin@example.com", "password-123");
    jdbcTemplate.update("UPDATE users SET role = 'ADMIN' WHERE email = ?", "admin@example.com");
    token = login("admin@example.com", "password-123");
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);

    var resp =
        restTemplate.exchange(
            "/admin/users", HttpMethod.GET, new HttpEntity<>(headers), List.class);

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp.getBody()).isNotEmpty();
  }
}
