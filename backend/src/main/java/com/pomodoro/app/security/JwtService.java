package com.pomodoro.app.security;

import com.pomodoro.app.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
  private final AppProperties appProperties;

  public JwtService(AppProperties appProperties) {
    this.appProperties = appProperties;
  }

  private SecretKey getKey() {
    return Keys.hmacShaKeyFor(appProperties.jwt().secret().getBytes(StandardCharsets.UTF_8));
  }

  public String generateAccessToken(Long userId, String role) {
    return Jwts.builder()
        .subject(userId.toString())
        .claims(Map.of("role", role, "type", "access"))
        .id(UUID.randomUUID().toString())
        .issuedAt(Date.from(Instant.now()))
        .expiration(
            Date.from(
                Instant.now().plusSeconds(appProperties.jwt().accessExpirationMinutes() * 60)))
        .signWith(getKey())
        .compact();
  }

  public String generateRefreshToken(Long userId) {
    return Jwts.builder()
        .subject(userId.toString())
        .claims(Map.of("type", "refresh"))
        .id(UUID.randomUUID().toString())
        .issuedAt(Date.from(Instant.now()))
        .expiration(
            Date.from(
                Instant.now().plusSeconds(appProperties.jwt().refreshExpirationDays() * 24 * 3600)))
        .signWith(getKey())
        .compact();
  }

  public Claims parse(String token) {
    return Jwts.parser().verifyWith(getKey()).build().parseSignedClaims(token).getPayload();
  }

  public Long extractUserId(String token) {
    return Long.parseLong(parse(token).getSubject());
  }

  public boolean isTokenType(String token, String expectedType) {
    return expectedType.equals(parse(token).get("type", String.class));
  }
}
