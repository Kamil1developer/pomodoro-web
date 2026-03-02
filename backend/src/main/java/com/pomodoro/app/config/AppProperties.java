package com.pomodoro.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Ai ai, String uploadsDir) {
  public record Jwt(String secret, long accessExpirationMinutes, long refreshExpirationDays) {}

  public record Ai(
      String mode,
      String openaiApiKey,
      String textModel,
      String visionModel,
      String imageModel,
      boolean useWebImageFeed,
      String ollamaApiUrl,
      String ollamaModel,
      String localImageApiUrl,
      int localImageSteps,
      String webImageApiUrl,
      int imageTimeoutSeconds,
      int chatTimeoutSeconds) {}
}
