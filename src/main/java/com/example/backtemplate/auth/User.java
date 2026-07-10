package com.example.backtemplate.auth;

import com.example.backtemplate.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User extends BaseEntity {

  @Column(nullable = false, unique = true)
  private String email;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Column(name = "email_verified", nullable = false)
  private boolean emailVerified = false;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Role role = Role.USER;

  @Column(name = "verification_token")
  private String verificationToken;

  @Column(name = "verification_token_expires_at")
  private Instant verificationTokenExpiresAt;

  @Column(name = "reset_token")
  private String resetToken;

  @Column(name = "reset_token_expires_at")
  private Instant resetTokenExpiresAt;
}
