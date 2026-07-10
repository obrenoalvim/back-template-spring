package com.example.backtemplate.auth;

import com.example.backtemplate.auth.dto.ForgotPasswordRequest;
import com.example.backtemplate.auth.dto.LoginRequest;
import com.example.backtemplate.auth.dto.LogoutRequest;
import com.example.backtemplate.auth.dto.RefreshRequest;
import com.example.backtemplate.auth.dto.RegisterRequest;
import com.example.backtemplate.auth.dto.ResetPasswordRequest;
import com.example.backtemplate.auth.dto.TokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "auth")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  @Operation(summary = "Register a new account")
  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  public void register(@Valid @RequestBody RegisterRequest req) {
    authService.register(req);
  }

  @Operation(summary = "Verify an email address with a token")
  @GetMapping("/verify-email")
  public void verifyEmail(@RequestParam String token) {
    authService.verifyEmail(token);
  }

  @Operation(summary = "Log in with email and password")
  @PostMapping("/login")
  public TokenResponse login(@Valid @RequestBody LoginRequest req) {
    return authService.login(req);
  }

  @Operation(summary = "Exchange a refresh token for a new access token")
  @PostMapping("/refresh")
  public TokenResponse refresh(@Valid @RequestBody RefreshRequest req) {
    return authService.refresh(req);
  }

  @Operation(summary = "Revoke a refresh token")
  @PostMapping("/logout")
  public void logout(@Valid @RequestBody LogoutRequest req) {
    authService.logout(req);
  }

  @Operation(summary = "Request a password reset email")
  @PostMapping("/forgot-password")
  public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
    authService.forgotPassword(req);
  }

  @Operation(summary = "Reset password with a token")
  @PostMapping("/reset-password")
  public void resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
    authService.resetPassword(req);
  }
}
