package com.pomodoro.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public class GoalDtos {
  public record GoalCreateRequest(
      @NotBlank @Size(max = 255) String title,
      @Size(max = 3000) String description,
      BigDecimal targetHours,
      LocalDate deadline) {}

  public record GoalUpdateRequest(
      @NotBlank @Size(max = 255) String title,
      @Size(max = 3000) String description,
      BigDecimal targetHours,
      LocalDate deadline) {}

  public record GoalResponse(
      Long id,
      String title,
      String description,
      BigDecimal targetHours,
      LocalDate deadline,
      Integer currentStreak,
      OffsetDateTime createdAt) {}

  public record TaskCreateRequest(@NotBlank @Size(max = 255) String title) {}

  public record TaskUpdateRequest(@NotBlank @Size(max = 255) String title, Boolean isDone) {}

  public record TaskResponse(
      Long id, Long goalId, String title, Boolean isDone, OffsetDateTime createdAt) {}

  public record FocusSessionResponse(
      Long id,
      Long goalId,
      OffsetDateTime startedAt,
      OffsetDateTime endedAt,
      Integer durationMinutes) {}

  public record GoalProgressResponse(
      long completedTasks, long allTasks, int totalFocusMinutes, int currentStreak) {}

  public record DailyStatResponse(
      LocalDate date, int completedTasks, int focusMinutes, int streak) {}

  public record GoalStatsResponse(Long goalId, List<DailyStatResponse> days) {}
}
