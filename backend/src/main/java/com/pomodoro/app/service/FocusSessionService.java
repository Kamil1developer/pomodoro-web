package com.pomodoro.app.service;

import com.pomodoro.app.dto.GoalDtos;
import com.pomodoro.app.entity.FocusSession;
import com.pomodoro.app.entity.Goal;
import com.pomodoro.app.exception.AppException;
import com.pomodoro.app.repository.FocusSessionRepository;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class FocusSessionService {
  private final GoalService goalService;
  private final DailyTaskPolicyService dailyTaskPolicyService;
  private final FocusSessionRepository focusSessionRepository;
  private final GoalCommitmentService goalCommitmentService;

  public FocusSessionService(
      GoalService goalService,
      DailyTaskPolicyService dailyTaskPolicyService,
      FocusSessionRepository focusSessionRepository,
      GoalCommitmentService goalCommitmentService) {
    this.goalService = goalService;
    this.dailyTaskPolicyService = dailyTaskPolicyService;
    this.focusSessionRepository = focusSessionRepository;
    this.goalCommitmentService = goalCommitmentService;
  }

  public GoalDtos.FocusSessionResponse start(Long userId, Long goalId) {
    dailyTaskPolicyService.ensureTasksCreatedToday(userId, goalId);
    Goal goal = goalService.ownedGoal(userId, goalId);
    focusSessionRepository
        .findFirstByGoalIdAndEndedAtIsNullOrderByStartedAtDesc(goalId)
        .ifPresent(
            s -> {
              throw new AppException(HttpStatus.BAD_REQUEST, "Active focus session already exists");
            });

    FocusSession session =
        focusSessionRepository.save(
            FocusSession.builder().goal(goal).startedAt(OffsetDateTime.now()).build());
    goalCommitmentService.onFocusSessionStarted(goal);
    return toResponse(session);
  }

  public GoalDtos.FocusSessionResponse stop(Long userId, Long goalId) {
    Goal goal = goalService.ownedGoal(userId, goalId);
    FocusSession session =
        focusSessionRepository
            .findFirstByGoalIdAndEndedAtIsNullOrderByStartedAtDesc(goalId)
            .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, "No active focus session"));
    session.setEndedAt(OffsetDateTime.now());
    session.setDurationMinutes(
        (int)
            Math.max(
                1, Duration.between(session.getStartedAt(), session.getEndedAt()).toMinutes()));
    FocusSession saved = focusSessionRepository.save(session);
    goalCommitmentService.onFocusSessionCompleted(goal, saved.getDurationMinutes());
    return toResponse(saved);
  }

  public List<GoalDtos.FocusSessionResponse> list(Long userId, Long goalId) {
    goalService.ownedGoal(userId, goalId);
    return focusSessionRepository.findByGoalIdOrderByStartedAtDesc(goalId).stream()
        .map(this::toResponse)
        .toList();
  }

  private GoalDtos.FocusSessionResponse toResponse(FocusSession s) {
    return new GoalDtos.FocusSessionResponse(
        s.getId(), s.getGoal().getId(), s.getStartedAt(), s.getEndedAt(), s.getDurationMinutes());
  }
}
