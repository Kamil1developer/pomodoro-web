package com.pomodoro.app.dto;

import com.pomodoro.app.enums.MotivationImageReportReason;
import com.pomodoro.app.enums.MotivationImageSource;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;

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
      Long id,
      Long goalId,
      String quoteText,
      String quoteTextRu,
      String quoteAuthor,
      String quoteDate) {}

  public record FeedRefreshResponse(
      java.util.List<MotivationResponse> images,
      java.util.List<DailyQuoteResponse> quotes,
      String refreshSessionId,
      Integer feedVersion,
      OffsetDateTime generatedAt,
      String refreshMessage) {}

  public record FavoriteRequest(Boolean isFavorite) {}

  public record MotivationImageResponse(
      Long id,
      String imageUrl,
      String title,
      String description,
      String caption,
      String displayQuote,
      String goalReason,
      OffsetDateTime createdAt) {}

  public record MotivationFeedResponse(
      List<MotivationImageResponse> images,
      DailyQuoteResponse quote,
      String recommendation,
      String refreshSessionId,
      Integer feedVersion,
      OffsetDateTime generatedAt,
      String refreshMessage) {}

  public record ReportMotivationImageRequest(
      @NotNull MotivationImageReportReason reason, @Size(max = 1000) String comment) {}

  public record FeedbackResponse(Long imageId, String status, String message) {}
}
