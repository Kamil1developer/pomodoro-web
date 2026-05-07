package com.pomodoro.app.dto;

import com.pomodoro.app.enums.GoalStatus;
import com.pomodoro.app.enums.RiskStatus;
import com.pomodoro.app.enums.WalletStatus;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;

public class ProfileDtos {
  public record ProfileResponse(
      Long userId,
      String email,
      String fullName,
      String avatarPath,
      ProfileStatsResponse stats,
      ProfileWalletResponse wallet,
      List<ProfileGoalItem> activeGoals,
      List<ProfileGoalHistoryItem> goalHistory) {}

  public record ProfileStatsResponse(
      long activeGoalsCount,
      long completedGoalsCount,
      long failedGoalsCount,
      int totalFocusMinutes,
      int bestStreak,
      Double averageDiscipline,
      RiskStatus riskSummary) {}

  public record ProfileWalletResponse(
      Integer balance, Integer initialBalance, Integer totalPenalties, WalletStatus status) {}

  public record ProfileGoalItem(
      Long goalId,
      String title,
      GoalStatus status,
      Integer currentStreak,
      Integer dailyTargetMinutes,
      Integer completedFocusMinutesToday,
      Integer remainingMinutesToday,
      Integer disciplineScore,
      RiskStatus riskStatus,
      Boolean moneyEnabled,
      Integer dailyPenaltyAmount,
      Integer totalPenaltyCharged,
      String moneyStatus,
      OffsetDateTime createdAt) {}

  public record ProfileGoalHistoryItem(
      Long goalId,
      String title,
      GoalStatus status,
      String failureReason,
      OffsetDateTime createdAt,
      OffsetDateTime completedAt,
      OffsetDateTime closedAt,
      Integer totalPenaltyCharged,
      Boolean loserBadge) {}

  public record UpdateProfileRequest(@Size(max = 255) String fullName) {}

  public record ProfileGoalsResponse(
      List<ProfileGoalItem> activeGoals, List<ProfileGoalHistoryItem> history) {}
}
