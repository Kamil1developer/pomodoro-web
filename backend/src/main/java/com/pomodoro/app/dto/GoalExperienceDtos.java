package com.pomodoro.app.dto;

import com.pomodoro.app.enums.CommitmentMoneyStatus;
import com.pomodoro.app.enums.CommitmentStatus;
import com.pomodoro.app.enums.GoalEventType;
import com.pomodoro.app.enums.ReportStatus;
import com.pomodoro.app.enums.RiskStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public class GoalExperienceDtos {
  public record CommitmentCreateRequest(
      @NotNull @Min(1) @Max(1440) Integer dailyTargetMinutes,
      @NotNull LocalDate startDate,
      LocalDate endDate,
      @Size(max = 255) String personalRewardTitle,
      @Size(max = 3000) String personalRewardDescription,
      Boolean moneyEnabled,
      @Min(0) Integer depositAmount,
      @Min(0) Integer dailyPenaltyAmount) {}

  public record CommitmentResponse(
      Long id,
      Long goalId,
      Integer dailyTargetMinutes,
      LocalDate startDate,
      LocalDate endDate,
      CommitmentStatus status,
      Integer disciplineScore,
      Integer currentStreak,
      Integer bestStreak,
      Integer completedDays,
      Integer missedDays,
      String personalRewardTitle,
      String personalRewardDescription,
      Boolean rewardUnlocked,
      RiskStatus riskStatus,
      Boolean moneyEnabled,
      Integer depositAmount,
      Integer dailyPenaltyAmount,
      Integer totalPenaltyCharged,
      CommitmentMoneyStatus moneyStatus,
      OffsetDateTime createdAt,
      OffsetDateTime updatedAt) {}

  public record TodayStatusResponse(
      Long goalId,
      String goalTitle,
      Integer dailyTargetMinutes,
      Integer completedFocusMinutesToday,
      Integer remainingMinutesToday,
      ReportStatus reportStatusToday,
      Boolean hasApprovedReportToday,
      Boolean isDailyTargetReached,
      Boolean isTodayCompleted,
      Integer disciplineScore,
      Integer currentStreak,
      RiskStatus riskStatus,
      Integer walletBalance,
      Integer dailyPenaltyAmount,
      Integer depositAmount,
      Integer totalPenaltyCharged,
      Boolean moneyEnabled,
      CommitmentMoneyStatus moneyStatus,
      String nextPenaltyWarning,
      String motivationalMessage,
      String nextRecommendedAction) {}

  public record ForecastResponse(
      Long goalId,
      BigDecimal targetHours,
      Integer totalFocusMinutes,
      Double averageDailyMinutes,
      Integer remainingMinutes,
      LocalDate estimatedCompletionDate,
      Boolean onTrack,
      String probabilityLabel,
      String explanation) {}

  public record GoalEventResponse(
      Long id,
      Long goalId,
      GoalEventType type,
      String title,
      String description,
      String oldValue,
      String newValue,
      OffsetDateTime createdAt) {}

  public record GoalExperienceResponse(
      GoalDtos.GoalResponse goal,
      CommitmentResponse commitment,
      TodayStatusResponse today,
      ForecastResponse forecast,
      List<GoalEventResponse> recentEvents,
      String aiRecommendation) {}
}
