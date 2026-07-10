package com.example.backtemplate.auth;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
  Optional<RefreshToken> findByTokenHash(String tokenHash);

  @Modifying
  @Transactional
  @Query("delete from RefreshToken r where r.tokenHash = :tokenHash")
  void deleteByTokenHash(@Param("tokenHash") String tokenHash);
}
