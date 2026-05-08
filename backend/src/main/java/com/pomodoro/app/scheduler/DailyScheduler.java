package com.pomodoro.app.scheduler;

import com.pomodoro.app.entity.DailySummary;
import com.pomodoro.app.entity.Goal;
import com.pomodoro.app.entity.Report;
import com.pomodoro.app.enums.ReportStatus;
import com.pomodoro.app.repository.*;
import com.pomodoro.app.service.GoalCommitmentService;
import com.pomodoro.app.service.MotivationService;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DailyScheduler {
  private final ReportRepository reportRepository;
  private final GoalRepository goalRepository;
  private final TaskItemRepository taskItemRepository;
  private final FocusSessionRepository focusSessionRepository;
  private final DailySummaryRepository dailySummaryRepository;
  private final MotivationService motivationService;
  private final GoalCommitmentService goalCommitmentService;

  public DailyScheduler(
      ReportRepository reportRepository,
      GoalRepository goalRepository,
      TaskItemRepository taskItemRepository,
      FocusSessionRepository focusSessionRepository,
      DailySummaryRepository dailySummaryRepository,
      MotivationService motivationService,
      GoalCommitmentService goalCommitmentService) {
    this.reportRepository = reportRepository;
    this.goalRepository = goalRepository;
    this.taskItemRepository = taskItemRepository;
    this.focusSessionRepository = focusSessionRepository;
    this.dailySummaryRepository = dailySummaryRepository;
    this.motivationService = motivationService;
    this.goalCommitmentService = goalCommitmentService;
  }

  @Scheduled(cron = "0 0 0 * * *")
  public void closeDay() {
    ZoneId zone = GoalCommitmentService.APPLICATION_ZONE;
    LocalDate today = LocalDate.now(zone);

    for (Report r : reportRepository.findByStatusAndReportDateBefore(ReportStatus.PENDING, today)) {
      r.setStatus(ReportStatus.OVERDUE);
      reportRepository.save(r);
    }

    for (Goal goal : goalRepository.findAll()) {
      boolean hasConfirmedYesterday =
          reportRepository.findByGoalIdAndReportDate(goal.getId(), today.minusDays(1)).stream()
              .anyMatch(r -> r.getStatus() == ReportStatus.CONFIRMED);
      goal.setCurrentStreak(hasConfirmedYesterday ? goal.getCurrentStreak() + 1 : 0);
      goalRepository.save(goal);

      int focusMinutes =
          focusSessionRepository.findByGoalIdOrderByStartedAtDesc(goal.getId()).stream()
              .filter(
                  fs ->
                      fs.getStartedAt() != null
                          && fs.getStartedAt().toLocalDate().isEqual(today.minusDays(1)))
              .filter(fs -> fs.getDurationMinutes() != null)
              .mapToInt(fs -> fs.getDurationMinutes())
              .sum();

      int completedTasks = (int) taskItemRepository.countByGoalIdAndIsDoneTrue(goal.getId());

      dailySummaryRepository
          .findByGoalIdAndSummaryDate(goal.getId(), today.minusDays(1))
          .orElseGet(
              () ->
                  dailySummaryRepository.save(
                      DailySummary.builder()
                          .goal(goal)
                          .summaryDate(today.minusDays(1))
                          .completedTasks(completedTasks)
                          .focusMinutes(focusMinutes)
                          .streak(goal.getCurrentStreak())
                          .createdAt(OffsetDateTime.now())
                          .build()));
    }

    LocalDate yesterday = today.minusDays(1);
    goalCommitmentService.processPreviousDaysThrough(yesterday);
    goalCommitmentService.processReportPenaltiesForActiveGoalsThrough(yesterday);

    log.info("Daily scheduler executed for {} in timezone {}", today, zone);
  }

  @Scheduled(cron = "0 0 */6 * * *")
  public void generateAutoMotivationImages() {
    int generated = 0;
    for (Goal goal : goalRepository.findAll()) {
      try {
        motivationService.generateAutoForGoal(goal);
        generated++;
      } catch (Exception e) {
        log.warn("Auto motivation generation failed for goal {}: {}", goal.getId(), e.getMessage());
      }
    }
    log.info("Auto motivation scheduler executed: generated {} images", generated);
  }

  @Scheduled(cron = "0 10 0 * * *")
  public void refreshDailyQuotes() {
    motivationService.ensureDailyQuotesForAllGoals(goalRepository.findAll(), LocalDate.now());
    log.info("Daily quote scheduler executed for {}", LocalDate.now());
  }
}
