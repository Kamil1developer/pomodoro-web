package com.pomodoro.app.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthDtos {
  public record RegisterRequest(
      @Email @NotBlank String email,
      @NotBlank @Size(min = 8, max = 128) String password,
      @Size(max = 255) String fullName) {}

  public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {}

  public record TokenResponse(
      String accessToken, String refreshToken, String tokenType, long expiresInSeconds) {}

  public record RefreshRequest(@NotBlank String refreshToken) {}

  public record LogoutRequest(@NotBlank String refreshToken) {}
}
