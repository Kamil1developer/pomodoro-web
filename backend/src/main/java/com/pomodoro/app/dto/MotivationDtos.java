package com.pomodoro.app.dto;

import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

public class MotivationDtos {
  public record GenerateRequest(@Size(max = 512) String styleOptions) {}

  public record MotivationResponse(
      Long id,
      Long goalId,
      String imagePath,
      String prompt,
      Boolean isFavorite,
      OffsetDateTime createdAt) {}

  public record FavoriteRequest(Boolean isFavorite) {}
}
