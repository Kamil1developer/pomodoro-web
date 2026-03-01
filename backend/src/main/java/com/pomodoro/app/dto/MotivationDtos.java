package com.pomodoro.app.dto;

import com.pomodoro.app.enums.MotivationImageSource;
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
      MotivationImageSource generatedBy,
      OffsetDateTime favoritedAt,
      OffsetDateTime pinnedUntil,
      Boolean isPinned,
      OffsetDateTime createdAt) {}

  public record DailyQuoteResponse(
      Long id, Long goalId, String quoteText, String quoteAuthor, String quoteDate) {}

  public record FavoriteRequest(Boolean isFavorite) {}
}
