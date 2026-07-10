package com.example.backtemplate.notes;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.backtemplate.AbstractIntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;

class NoteControllerIntegrationTest extends AbstractIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private JdbcTemplate jdbcTemplate;

  private String loginAndGetToken(String email, String password) {
    restTemplate.postForEntity(
        "/auth/register", Map.of("email", email, "password", password), Void.class);
    jdbcTemplate.update("UPDATE users SET email_verified = true WHERE email = ?", email);
    var resp =
        restTemplate.postForEntity(
            "/auth/login", Map.of("email", email, "password", password), Map.class);
    return (String) resp.getBody().get("accessToken");
  }

  @Test
  void fullCrudRoundTrip() {
    String token = loginAndGetToken("crud@example.com", "s3cret-pw");
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    headers.setContentType(MediaType.APPLICATION_JSON);

    var createBody = Map.of("title", "Hello", "content", "World");
    var createResp =
        restTemplate.exchange(
            "/api/notes",
            HttpMethod.POST,
            new HttpEntity<>(createBody, headers),
            java.util.Map.class);
    assertThat(createResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    String noteId = (String) createResp.getBody().get("id");

    var listResp =
        restTemplate.exchange(
            "/api/notes", HttpMethod.GET, new HttpEntity<>(headers), java.util.List.class);
    assertThat(listResp.getBody()).hasSize(1);

    var getResp =
        restTemplate.exchange(
            "/api/notes/" + noteId, HttpMethod.GET, new HttpEntity<>(headers), java.util.Map.class);
    assertThat(getResp.getBody().get("title")).isEqualTo("Hello");

    var updateBody = Map.of("title", "Updated", "content", "World");
    restTemplate.exchange(
        "/api/notes/" + noteId,
        HttpMethod.PUT,
        new HttpEntity<>(updateBody, headers),
        java.util.Map.class);

    var deleteResp =
        restTemplate.exchange(
            "/api/notes/" + noteId, HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
    assertThat(deleteResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
  }
}
