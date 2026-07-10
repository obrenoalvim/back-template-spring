package com.example.backtemplate.auth;

import com.example.backtemplate.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtService {

  private final AppProperties appProperties;

  private Key signingKey() {
    return Keys.hmacShaKeyFor(appProperties.getJwt().getSecret().getBytes());
  }

  public String generateAccessToken(UUID userId, String email, String role) {
    Instant now = Instant.now();
    return Jwts.builder()
        .subject(userId.toString())
        .claim("email", email)
        .claim("role", role)
        .claim("type", "access")
        .issuedAt(Date.from(now))
        .expiration(
            Date.from(now.plus(Duration.ofMinutes(appProperties.getJwt().getAccessTtlMinutes()))))
        .signWith(signingKey())
        .compact();
  }

  public String generateRefreshToken(UUID userId) {
    Instant now = Instant.now();
    return Jwts.builder()
        .subject(userId.toString())
        .claim("type", "refresh")
        // JWT claims are second-precision — without a random jti, two refresh tokens
        // issued for the same user within the same second are byte-identical, which
        // silently defeats rotation (the "new" token collides with the one just
        // revoked). id() guarantees uniqueness regardless of timing.
        .id(UUID.randomUUID().toString())
        .issuedAt(Date.from(now))
        .expiration(
            Date.from(now.plus(Duration.ofDays(appProperties.getJwt().getRefreshTtlDays()))))
        .signWith(signingKey())
        .compact();
  }

  public Jws<Claims> parse(String token) {
    return Jwts.parser()
        .verifyWith((javax.crypto.SecretKey) signingKey())
        .build()
        .parseSignedClaims(token);
  }
}
