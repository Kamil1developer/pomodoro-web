package com.pomodoro.app.repository;

import com.pomodoro.app.entity.RefreshToken;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
  Optional<RefreshToken> findByToken(String token);

  void deleteByToken(String token);

  void deleteByExpiresAtBefore(OffsetDateTime dateTime);
}
