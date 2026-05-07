package com.pomodoro.app.service;

import com.pomodoro.app.config.AppProperties;
import com.pomodoro.app.dto.AuthDtos;
import com.pomodoro.app.entity.RefreshToken;
import com.pomodoro.app.entity.User;
import com.pomodoro.app.enums.Role;
import com.pomodoro.app.exception.AppException;
import com.pomodoro.app.repository.RefreshTokenRepository;
import com.pomodoro.app.repository.UserRepository;
import com.pomodoro.app.security.JwtService;
import io.jsonwebtoken.JwtException;
import java.time.OffsetDateTime;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {
  private final UserRepository userRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final AppProperties appProperties;
  private final WalletService walletService;

  public AuthService(
      UserRepository userRepository,
      RefreshTokenRepository refreshTokenRepository,
      PasswordEncoder passwordEncoder,
      JwtService jwtService,
      AppProperties appProperties,
      WalletService walletService) {
    this.userRepository = userRepository;
    this.refreshTokenRepository = refreshTokenRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.appProperties = appProperties;
    this.walletService = walletService;
  }

  public AuthDtos.TokenResponse register(AuthDtos.RegisterRequest request) {
    if (userRepository.existsByEmail(request.email())) {
      throw new AppException(HttpStatus.CONFLICT, "Email already exists");
    }
    User user =
        userRepository.save(
            User.builder()
                .email(request.email().toLowerCase())
                .fullName(resolveFullName(request.fullName(), request.email()))
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(Role.USER)
                .createdAt(OffsetDateTime.now())
                .build());
    walletService.initializeWalletForNewUser(user);

    return createTokenPair(user);
  }

  public AuthDtos.TokenResponse login(AuthDtos.LoginRequest request) {
    User user =
        userRepository
            .findByEmail(request.email().toLowerCase())
            .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throw new AppException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }

    return createTokenPair(user);
  }

  @Transactional(readOnly = false)
  public AuthDtos.TokenResponse refresh(AuthDtos.RefreshRequest request) {
    RefreshToken token =
        refreshTokenRepository
            .findByToken(request.refreshToken())
            .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

    if (token.getExpiresAt().isBefore(OffsetDateTime.now())) {
      refreshTokenRepository.delete(token);
      throw new AppException(HttpStatus.UNAUTHORIZED, "Refresh token expired");
    }

    try {
      if (!jwtService.isTokenType(request.refreshToken(), "refresh")) {
        throw new AppException(HttpStatus.UNAUTHORIZED, "Invalid token type");
      }
    } catch (JwtException e) {
      throw new AppException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
    }

    return createTokenPair(token.getUser());
  }

  public void logout(AuthDtos.LogoutRequest request) {
    refreshTokenRepository.deleteByToken(request.refreshToken());
  }

  private AuthDtos.TokenResponse createTokenPair(User user) {
    String accessToken = jwtService.generateAccessToken(user.getId(), user.getRole().name());
    String refreshToken = jwtService.generateRefreshToken(user.getId());

    refreshTokenRepository.save(
        RefreshToken.builder()
            .user(user)
            .token(refreshToken)
            .createdAt(OffsetDateTime.now())
            .expiresAt(OffsetDateTime.now().plusDays(appProperties.jwt().refreshExpirationDays()))
            .build());
    refreshTokenRepository.deleteByExpiresAtBefore(OffsetDateTime.now());

    return new AuthDtos.TokenResponse(
        accessToken, refreshToken, "Bearer", appProperties.jwt().accessExpirationMinutes() * 60);
  }

  private String resolveFullName(String fullName, String email) {
    if (fullName != null && !fullName.isBlank()) {
      return fullName.trim();
    }
    String localPart = email.toLowerCase(Locale.ROOT).split("@")[0];
    if (localPart.isBlank()) {
      return "User";
    }
    return Character.toUpperCase(localPart.charAt(0)) + localPart.substring(1);
  }
}
