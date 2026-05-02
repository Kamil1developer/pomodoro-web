package com.pomodoro.app.service;

import com.pomodoro.app.dto.GoalDtos;
import com.pomodoro.app.entity.DailySummary;
import com.pomodoro.app.entity.FocusSession;
import com.pomodoro.app.entity.Goal;
import com.pomodoro.app.entity.TaskItem;
import com.pomodoro.app.enums.GoalEventType;
import com.pomodoro.app.exception.AppException;
import com.pomodoro.app.repository.DailySummaryRepository;
import com.pomodoro.app.repository.FocusSessionRepository;
import com.pomodoro.app.repository.GoalRepository;
import com.pomodoro.app.repository.TaskItemRepository;
import com.pomodoro.app.repository.UserRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class GoalService {
  private static final String DEFAULT_THEME_COLOR = "#dff6e5";

  private final GoalRepository goalRepository;
  private final UserRepository userRepository;
  private final TaskItemRepository taskItemRepository;
  private final FocusSessionRepository focusSessionRepository;
  private final DailySummaryRepository dailySummaryRepository;
  private final GoalEventService goalEventService;

  public GoalService(
      GoalRepository goalRepository,
      UserRepository userRepository,
      TaskItemRepository taskItemRepository,
      FocusSessionRepository focusSessionRepository,
      DailySummaryRepository dailySummaryRepository,
      GoalEventService goalEventService) {
    this.goalRepository = goalRepository;
    this.userRepository = userRepository;
    this.taskItemRepository = taskItemRepository;
    this.focusSessionRepository = focusSessionRepository;
    this.dailySummaryRepository = dailySummaryRepository;
    this.goalEventService = goalEventService;
  }

  public List<GoalDtos.GoalResponse> getGoals(Long userId) {
    return goalRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
        .map(this::toGoalResponse)
        .toList();
  }

  public GoalDtos.GoalResponse getGoal(Long userId, Long goalId) {
    Goal goal = ownedGoal(userId, goalId);
    return toGoalResponse(goal);
  }

  public Goal ownedGoal(Long userId, Long goalId) {
    return goalRepository
        .findByIdAndUserId(goalId, userId)
        .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Goal not found"));
  }

  public GoalDtos.GoalResponse createGoal(Long userId, GoalDtos.GoalCreateRequest request) {
    Goal goal =
        Goal.builder()
            .user(
                userRepository
                    .findById(userId)
                    .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found")))
            .title(request.title())
            .description(request.description())
            .targetHours(request.targetHours())
            .deadline(request.deadline())
            .themeColor(request.themeColor() == null ? DEFAULT_THEME_COLOR : request.themeColor())
            .currentStreak(0)
            .createdAt(OffsetDateTime.now())
            .build();
    Goal saved = goalRepository.save(goal);
    goalEventService.createEvent(
        saved,
        null,
        GoalEventType.GOAL_CREATED,
        "Цель создана",
        "Добавлена новая цель \"%s\".".formatted(saved.getTitle()),
        null,
        saved.getTitle());
    return toGoalResponse(saved);
  }

  public GoalDtos.GoalResponse updateGoal(
      Long userId, Long goalId, GoalDtos.GoalUpdateRequest request) {
    Goal goal = ownedGoal(userId, goalId);
    goal.setTitle(request.title());
    goal.setDescription(request.description());
    goal.setTargetHours(request.targetHours());
    goal.setDeadline(request.deadline());
    goal.setThemeColor(request.themeColor() == null ? goal.getThemeColor() : request.themeColor());
    return toGoalResponse(goalRepository.save(goal));
  }

  public void deleteGoal(Long userId, Long goalId) {
    Goal goal = ownedGoal(userId, goalId);
    goalRepository.delete(goal);
  }

  public List<GoalDtos.TaskResponse> getTasks(Long userId, Long goalId) {
    ownedGoal(userId, goalId);
    return taskItemRepository.findByGoalIdOrderByCreatedAtDesc(goalId).stream()
        .map(this::toTaskResponse)
        .toList();
  }

  public GoalDtos.TaskResponse createTask(
      Long userId, Long goalId, GoalDtos.TaskCreateRequest request) {
    Goal goal = ownedGoal(userId, goalId);
    TaskItem task =
        taskItemRepository.save(
            TaskItem.builder()
                .goal(goal)
                .title(request.title())
                .isDone(false)
                .createdAt(OffsetDateTime.now())
                .build());
    return toTaskResponse(task);
  }

  public GoalDtos.TaskResponse updateTask(
      Long userId, Long goalId, Long taskId, GoalDtos.TaskUpdateRequest request) {
    ownedGoal(userId, goalId);
    TaskItem task =
        taskItemRepository
            .findByIdAndGoalId(taskId, goalId)
            .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Task not found"));
    task.setTitle(request.title());
    if (request.isDone() != null) {
      task.setIsDone(request.isDone());
    }
    return toTaskResponse(taskItemRepository.save(task));
  }

  public void deleteTask(Long userId, Long goalId, Long taskId) {
    ownedGoal(userId, goalId);
    TaskItem task =
        taskItemRepository
            .findByIdAndGoalId(taskId, goalId)
            .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Task not found"));
    taskItemRepository.delete(task);
  }

  public GoalDtos.GoalProgressResponse getProgress(Long userId, Long goalId) {
    Goal goal = ownedGoal(userId, goalId);
    long done = taskItemRepository.countByGoalIdAndIsDoneTrue(goalId);
    long all = taskItemRepository.countByGoalId(goalId);
    int focusMinutes =
        focusSessionRepository.findByGoalIdOrderByStartedAtDesc(goalId).stream()
            .filter(fs -> fs.getDurationMinutes() != null)
            .mapToInt(FocusSession::getDurationMinutes)
            .sum();
    return new GoalDtos.GoalProgressResponse(done, all, focusMinutes, goal.getCurrentStreak());
  }

  public GoalDtos.GoalStatsResponse getStats(Long userId, Long goalId) {
    ownedGoal(userId, goalId);
    List<GoalDtos.DailyStatResponse> days =
        dailySummaryRepository.findByGoalIdOrderBySummaryDateAsc(goalId).stream()
            .map(this::toDailyStatResponse)
            .toList();
    return new GoalDtos.GoalStatsResponse(goalId, days);
  }

  private GoalDtos.GoalResponse toGoalResponse(Goal g) {
    return new GoalDtos.GoalResponse(
        g.getId(),
        g.getTitle(),
        g.getDescription(),
        g.getTargetHours(),
        g.getDeadline(),
        g.getThemeColor(),
        g.getCurrentStreak(),
        g.getCreatedAt());
  }

  private GoalDtos.TaskResponse toTaskResponse(TaskItem t) {
    return new GoalDtos.TaskResponse(
        t.getId(), t.getGoal().getId(), t.getTitle(), t.getIsDone(), t.getCreatedAt());
  }

  private GoalDtos.DailyStatResponse toDailyStatResponse(DailySummary summary) {
    return new GoalDtos.DailyStatResponse(
        summary.getSummaryDate(),
        summary.getCompletedTasks(),
        summary.getFocusMinutes(),
        summary.getStreak());
  }
}
