package com.example.backtemplate.auth;

import com.example.backtemplate.auth.dto.ForgotPasswordRequest;
import com.example.backtemplate.auth.dto.LoginRequest;
import com.example.backtemplate.auth.dto.LogoutRequest;
import com.example.backtemplate.auth.dto.RefreshRequest;
import com.example.backtemplate.auth.dto.RegisterRequest;
import com.example.backtemplate.auth.dto.ResetPasswordRequest;
import com.example.backtemplate.auth.dto.TokenResponse;
import com.example.backtemplate.common.ApiException;
import com.example.backtemplate.config.AppProperties;
import com.example.backtemplate.email.EmailService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordService passwordService;
  private final EmailService emailService;
  private final JwtService jwtService;
  private final RefreshTokenRepository refreshTokenRepository;
  private final AppProperties appProperties;

  private static String hash(String token) {
    try {
      var digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  private TokenResponse issueTokenPair(User user) {
    String refreshToken = jwtService.generateRefreshToken(user.getId());

    var record = new RefreshToken();
    record.setTokenHash(hash(refreshToken));
    record.setUserId(user.getId());
    record.setExpiresAt(
        Instant.now().plus(appProperties.getJwt().getRefreshTtlDays(), ChronoUnit.DAYS));
    refreshTokenRepository.save(record);

    return new TokenResponse(
        jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name()),
        refreshToken);
  }

  public void register(RegisterRequest req) {
    userRepository
        .findByEmail(req.email())
        .ifPresent(
            u -> {
              throw ApiException.conflict("Email already registered");
            });

    User user = new User();
    user.setEmail(req.email());
    user.setPasswordHash(passwordService.hash(req.password()));
    user.setEmailVerified(false);
    user.setVerificationToken(UUID.randomUUID().toString());
    user.setVerificationTokenExpiresAt(Instant.now().plusSeconds(24 * 3600));
    userRepository.save(user);

    emailService.send(
        user.getEmail(), "Verify your email", "Verification token: " + user.getVerificationToken());
  }

  public void verifyEmail(String token) {
    User user =
        userRepository
            .findByVerificationToken(token)
            .orElseThrow(() -> ApiException.notFound("Invalid verification token"));

    if (user.getVerificationTokenExpiresAt().isBefore(Instant.now())) {
      throw ApiException.conflict("Verification token expired");
    }

    user.setEmailVerified(true);
    user.setVerificationToken(null);
    user.setVerificationTokenExpiresAt(null);
    userRepository.save(user);
  }

  public TokenResponse login(LoginRequest req) {
    User user =
        userRepository
            .findByEmail(req.email())
            .orElseThrow(() -> ApiException.unauthorized("Invalid credentials"));

    if (!passwordService.matches(req.password(), user.getPasswordHash())) {
      throw ApiException.unauthorized("Invalid credentials");
    }
    if (!user.isEmailVerified()) {
      throw ApiException.unauthorized("Email not verified");
    }

    return issueTokenPair(user);
  }

  public TokenResponse refresh(RefreshRequest req) {
    var claims = jwtService.parse(req.refreshToken()).getPayload();
    if (!"refresh".equals(claims.get("type", String.class))) {
      throw ApiException.unauthorized("Invalid refresh token");
    }

    String tokenHash = hash(req.refreshToken());
    RefreshToken record =
        refreshTokenRepository
            .findByTokenHash(tokenHash)
            .filter(r -> r.getExpiresAt().isAfter(Instant.now()))
            .orElseThrow(() -> ApiException.unauthorized("Invalid or expired refresh token"));

    UUID userId = UUID.fromString(claims.getSubject());
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> ApiException.unauthorized("Invalid refresh token"));

    // Rotation: the old token is deleted the moment it's used, so a stolen-and-replayed
    // refresh token stops working after the legitimate client's next refresh.
    refreshTokenRepository.delete(record);

    return issueTokenPair(user);
  }

  public void logout(LogoutRequest req) {
    // Idempotent — a missing/already-revoked token still returns normally, don't leak.
    refreshTokenRepository.deleteByTokenHash(hash(req.refreshToken()));
  }

  public void forgotPassword(ForgotPasswordRequest req) {
    userRepository
        .findByEmail(req.email())
        .ifPresent(
            user -> {
              user.setResetToken(UUID.randomUUID().toString());
              user.setResetTokenExpiresAt(Instant.now().plusSeconds(3600));
              userRepository.save(user);
              emailService.send(
                  user.getEmail(), "Reset your password", "Reset token: " + user.getResetToken());
            });
    // always returns normally, whether or not the email exists -- avoids user enumeration
  }

  public void resetPassword(ResetPasswordRequest req) {
    User user =
        userRepository
            .findByResetToken(req.token())
            .orElseThrow(() -> ApiException.notFound("Invalid reset token"));

    if (user.getResetTokenExpiresAt().isBefore(Instant.now())) {
      throw ApiException.conflict("Reset token expired");
    }

    user.setPasswordHash(passwordService.hash(req.newPassword()));
    user.setResetToken(null);
    user.setResetTokenExpiresAt(null);
    userRepository.save(user);
  }
}
