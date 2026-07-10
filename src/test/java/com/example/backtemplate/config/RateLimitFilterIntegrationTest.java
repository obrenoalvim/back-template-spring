package com.example.backtemplate.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.backtemplate.AbstractIntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;

class RateLimitFilterIntegrationTest extends AbstractIntegrationTest {

    @Autowired private TestRestTemplate restTemplate;

    @Test
    void sixthLoginAttemptWithinAMinuteIsRateLimited() {
        var body = Map.of("email", "ratelimit@example.com", "password", "wrong-pw");

        for (int i = 0; i < 5; i++) {
            restTemplate.postForEntity("/auth/login", body, Void.class);
        }
        var sixth = restTemplate.postForEntity("/auth/login", body, Void.class);

        assertThat(sixth.getStatusCode().value()).isEqualTo(429);
    }
}
