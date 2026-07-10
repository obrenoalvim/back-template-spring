package com.example.backtemplate.account;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.backtemplate.AbstractIntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;

class AccountControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private JdbcTemplate jdbcTemplate;

    private String login(String email, String password) {
        restTemplate.postForEntity("/auth/register", Map.of("email", email, "password", password), Void.class);
        jdbcTemplate.update("UPDATE users SET email_verified = true WHERE email = ?", email);
        var resp =
                restTemplate.postForEntity(
                        "/auth/login", Map.of("email", email, "password", password), Map.class);
        return (String) resp.getBody().get("accessToken");
    }

    @Test
    void changePasswordThenDeleteAccount() {
        String token = login("account@example.com", "old-pw-123");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        var changeResp =
                restTemplate.exchange(
                        "/account/password",
                        HttpMethod.PATCH,
                        new HttpEntity<>(
                                Map.of("currentPassword", "old-pw-123", "newPassword", "new-pw-456"), headers),
                        Void.class);
        assertThat(changeResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        var deleteResp =
                restTemplate.exchange("/account", HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
        assertThat(deleteResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        Integer count =
                jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM users WHERE email = ?", Integer.class, "account@example.com");
        assertThat(count).isEqualTo(0);
    }
}
