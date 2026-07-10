package com.example.backtemplate.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.backtemplate.config.AppProperties;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private final AppProperties props = new AppProperties();

    {
        var jwt = new AppProperties.Jwt();
        jwt.setSecret("a".repeat(32));
        jwt.setAccessTtlMinutes(15);
        jwt.setRefreshTtlDays(30);
        props.setJwt(jwt);
    }

    private final JwtService jwtService = new JwtService(props);

    @Test
    void generatesAndParsesAccessToken() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateAccessToken(userId, "user@example.com");

        var claims = jwtService.parse(token).getPayload();

        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.get("email", String.class)).isEqualTo("user@example.com");
    }

    @Test
    void rejectsTamperedToken() {
        String token = jwtService.generateAccessToken(UUID.randomUUID(), "user@example.com");
        String tampered = token.substring(0, token.length() - 1) + "x";

        assertThatThrownBy(() -> jwtService.parse(tampered))
                .isInstanceOf(io.jsonwebtoken.JwtException.class);
    }
}
