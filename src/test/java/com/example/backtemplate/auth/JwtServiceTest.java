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
    // Flip a character in the middle of the payload segment, not the last character of the
    // whole token: base64url's final character can carry unused padding bits, so swapping it
    // sometimes decodes to the exact same bytes and the "tampered" token verifies anyway.
    int mid = token.length() / 2;
    char flipped = token.charAt(mid) == 'a' ? 'b' : 'a';
    String tampered = token.substring(0, mid) + flipped + token.substring(mid + 1);

    assertThatThrownBy(() -> jwtService.parse(tampered))
        .isInstanceOf(io.jsonwebtoken.JwtException.class);
  }
}
