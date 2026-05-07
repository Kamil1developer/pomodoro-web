package com.pomodoro.app.service;

import com.pomodoro.app.dto.GoalDtos;
import com.pomodoro.app.entity.DailySummary;
import com.pomodoro.app.entity.FocusSession;
import com.pomodoro.app.entity.Goal;
import com.pomodoro.app.entity.TaskItem;
import com.pomodoro.app.entity.User;
import com.pomodoro.app.enums.GoalEventType;
import com.pomodoro.app.enums.GoalStatus;
import com.pomodoro.app.exception.AppException;
import com.pomodoro.app.repository.DailySummaryRepository;
import com.pomodoro.app.repository.FocusSessionRepository;
import com.pomodoro.app.repository.GoalRepository;
import com.pomodoro.app.repository.TaskItemRepository;
import com.pomodoro.app.repository.UserRepository;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class GoalService {
  private static final String DEFAULT_THEME_COLOR = "#dff6e5";
  private static final Set<String> GOAL_STOP_WORDS =
      Set.of(
          "хочу",
          "начать",
          "начну",
          "изучать",
          "изучение",
          "изучить",
          "выучить",
          "учить",
          "сделать",
          "делать",
          "goal",
          "цель",
          "мой",
          "моя",
          "my",
          "to",
          "the",
          "a",
          "an");

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

  @Transactional(readOnly = true)
  public List<GoalDtos.GoalResponse> getGoals(Long userId) {
    return goalRepository
        .findByUserIdAndStatusOrderByCreatedAtDesc(userId, GoalStatus.ACTIVE)
        .stream()
        .map(this::toGoalResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public GoalDtos.GoalResponse getGoal(Long userId, Long goalId) {
    Goal goal = ownedGoal(userId, goalId);
    return toGoalResponse(goal);
  }

  @Transactional(readOnly = true)
  public Goal ownedGoal(Long userId, Long goalId) {
    return goalRepository
        .findByIdAndUserId(goalId, userId)
        .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Цель не найдена."));
  }

  public GoalDtos.GoalResponse createGoal(Long userId, GoalDtos.GoalCreateRequest request) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Пользователь не найден."));
    ensureNoSimilarActiveGoal(userId, request.title(), null);

    Goal goal =
        Goal.builder()
            .user(user)
            .title(cleanText(request.title()))
            .description(blankToNull(request.description()))
            .targetHours(request.targetHours())
            .deadline(request.deadline())
            .themeColor(request.themeColor() == null ? DEFAULT_THEME_COLOR : request.themeColor())
            .status(GoalStatus.ACTIVE)
            .currentStreak(0)
            .completedAt(null)
            .closedAt(null)
            .failureReason(null)
            .createdAt(OffsetDateTime.now())
            .build();
    Goal saved = goalRepository.save(goal);
    goalEventService.createEvent(
        saved,
        null,
        GoalEventType.GOAL_CREATED,
        "Цель создана",
        "Добавлена новая цель «%s».".formatted(saved.getTitle()),
        null,
        saved.getTitle());
    return toGoalResponse(saved);
  }

  public GoalDtos.GoalResponse updateGoal(
      Long userId, Long goalId, GoalDtos.GoalUpdateRequest request) {
    Goal goal = ownedGoal(userId, goalId);
    if (goal.getStatus() != GoalStatus.ACTIVE) {
      throw new AppException(HttpStatus.BAD_REQUEST, "Редактировать можно только активную цель.");
    }
    ensureNoSimilarActiveGoal(userId, request.title(), goalId);
    goal.setTitle(cleanText(request.title()));
    goal.setDescription(blankToNull(request.description()));
    goal.setTargetHours(request.targetHours());
    goal.setDeadline(request.deadline());
    goal.setThemeColor(request.themeColor() == null ? goal.getThemeColor() : request.themeColor());
    return toGoalResponse(goalRepository.save(goal));
  }

  public void deleteGoal(Long userId, Long goalId) {
    Goal goal = ownedGoal(userId, goalId);
    if (goal.getStatus() == GoalStatus.ACTIVE) {
      throw new AppException(
          HttpStatus.BAD_REQUEST,
          "Активную цель нельзя удалять бесследно. Закройте её как невыполненную с указанием причины.");
    }
    goal.setStatus(GoalStatus.ARCHIVED);
    goalRepository.save(goal);
  }

  public GoalDtos.GoalResponse closeFailedGoal(Long userId, Long goalId, String reason) {
    Goal goal = ownedGoal(userId, goalId);
    if (goal.getStatus() != GoalStatus.ACTIVE) {
      throw new AppException(
          HttpStatus.BAD_REQUEST, "Закрыть как невыполненную можно только активную цель.");
    }
    String normalizedReason = cleanText(reason);
    goal.setStatus(GoalStatus.FAILED);
    goal.setClosedAt(OffsetDateTime.now());
    goal.setFailureReason(normalizedReason);
    Goal saved = goalRepository.save(goal);
    goalEventService.createEvent(
        saved,
        null,
        GoalEventType.DAY_MISSED,
        "Цель закрыта как невыполненная",
        "Пользователь закрыл цель как невыполненную. Причина: %s".formatted(normalizedReason),
        GoalStatus.ACTIVE.name(),
        GoalStatus.FAILED.name());
    return toGoalResponse(saved);
  }

  public Goal markCompleted(Goal goal, String description) {
    if (goal.getStatus() == GoalStatus.COMPLETED) {
      return goal;
    }
    goal.setStatus(GoalStatus.COMPLETED);
    goal.setCompletedAt(OffsetDateTime.now());
    goal.setClosedAt(OffsetDateTime.now());
    goal.setFailureReason(null);
    Goal saved = goalRepository.save(goal);
    goalEventService.createEvent(
        saved,
        null,
        GoalEventType.REWARD_UNLOCKED,
        "Цель завершена",
        description,
        GoalStatus.ACTIVE.name(),
        GoalStatus.COMPLETED.name());
    return saved;
  }

  public Goal markFailed(Goal goal, String reason) {
    if (goal.getStatus() == GoalStatus.FAILED) {
      return goal;
    }
    goal.setStatus(GoalStatus.FAILED);
    goal.setClosedAt(OffsetDateTime.now());
    goal.setFailureReason(reason);
    Goal saved = goalRepository.save(goal);
    goalEventService.createEvent(
        saved,
        null,
        GoalEventType.DAY_MISSED,
        "Цель завершилась неуспешно",
        reason,
        GoalStatus.ACTIVE.name(),
        GoalStatus.FAILED.name());
    return saved;
  }

  @Transactional(readOnly = true)
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
                .title(cleanText(request.title()))
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
            .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Задача не найдена."));
    task.setTitle(cleanText(request.title()));
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
            .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Задача не найдена."));
    taskItemRepository.delete(task);
  }

  @Transactional(readOnly = true)
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

  @Transactional(readOnly = true)
  public GoalDtos.GoalStatsResponse getStats(Long userId, Long goalId) {
    ownedGoal(userId, goalId);
    List<GoalDtos.DailyStatResponse> days =
        dailySummaryRepository.findByGoalIdOrderBySummaryDateAsc(goalId).stream()
            .map(this::toDailyStatResponse)
            .toList();
    return new GoalDtos.GoalStatsResponse(goalId, days);
  }

  private void ensureNoSimilarActiveGoal(Long userId, String candidateTitle, Long excludeGoalId) {
    String normalizedCandidate = normalizeGoalText(candidateTitle);
    Set<String> candidateTokens = normalizeGoalTokens(candidateTitle);

    for (Goal existingGoal :
        goalRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, GoalStatus.ACTIVE)) {
      if (excludeGoalId != null && excludeGoalId.equals(existingGoal.getId())) {
        continue;
      }
      String normalizedExisting = normalizeGoalText(existingGoal.getTitle());
      Set<String> existingTokens = normalizeGoalTokens(existingGoal.getTitle());
      if (normalizedCandidate.equals(normalizedExisting)
          || normalizedExisting.contains(normalizedCandidate)
          || normalizedCandidate.contains(normalizedExisting)
          || similarity(candidateTokens, existingTokens) >= 0.65d) {
        throw new AppException(
            HttpStatus.CONFLICT,
            "Похожая активная цель уже существует: «%s». Сначала завершите или закройте её."
                .formatted(existingGoal.getTitle()));
      }
    }
  }

  private double similarity(Set<String> left, Set<String> right) {
    if (left.isEmpty() || right.isEmpty()) {
      return 0d;
    }
    Set<String> intersection = new HashSet<>(left);
    intersection.retainAll(right);
    Set<String> union = new HashSet<>(left);
    union.addAll(right);
    return union.isEmpty() ? 0d : (double) intersection.size() / union.size();
  }

  private String normalizeGoalText(String text) {
    return String.join(" ", normalizeGoalTokens(text));
  }

  private Set<String> normalizeGoalTokens(String text) {
    String normalized =
        cleanText(text)
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^\\p{L}\\p{Nd}\\s]", " ")
            .replaceAll("\\s+", " ")
            .trim();
    Set<String> tokens = new HashSet<>();
    if (normalized.isBlank()) {
      return tokens;
    }
    for (String token : normalized.split(" ")) {
      String canonical = canonicalGoalToken(token);
      if (canonical.length() < 3 || GOAL_STOP_WORDS.contains(canonical)) {
        continue;
      }
      tokens.add(canonical);
    }
    return tokens;
  }

  private String canonicalGoalToken(String token) {
    if (token.startsWith("англий")) {
      return "английский";
    }
    if (token.startsWith("изуч") || token.startsWith("уч") || token.startsWith("выуч")) {
      return "учить";
    }
    if (token.startsWith("спорт") || token.startsWith("трен") || token.startsWith("fitness")) {
      return "спорт";
    }
    if (token.startsWith("java")) {
      return "java";
    }
    if (token.startsWith("програм") || token.startsWith("code") || token.startsWith("develop")) {
      return "программирование";
    }
    if (token.equals("язык")) {
      return "язык";
    }
    return token;
  }

  private GoalDtos.GoalResponse toGoalResponse(Goal g) {
    return new GoalDtos.GoalResponse(
        g.getId(),
        g.getTitle(),
        g.getDescription(),
        g.getTargetHours(),
        g.getDeadline(),
        g.getThemeColor(),
        g.getStatus(),
        g.getCurrentStreak(),
        g.getCompletedAt(),
        g.getClosedAt(),
        g.getFailureReason(),
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

  private String cleanText(String value) {
    return value == null ? "" : value.trim().replaceAll("\\s+", " ");
  }

  private String blankToNull(String value) {
    String cleaned = cleanText(value);
    return cleaned.isBlank() ? null : cleaned;
  }
}
