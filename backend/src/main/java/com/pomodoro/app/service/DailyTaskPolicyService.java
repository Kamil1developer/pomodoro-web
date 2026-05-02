package com.pomodoro.app.service;

import com.pomodoro.app.entity.Goal;
import com.pomodoro.app.entity.TaskItem;
import com.pomodoro.app.exception.AppException;
import com.pomodoro.app.repository.TaskItemRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class DailyTaskPolicyService {
  private final GoalService goalService;
  private final TaskItemRepository taskItemRepository;

  public DailyTaskPolicyService(GoalService goalService, TaskItemRepository taskItemRepository) {
    this.goalService = goalService;
    this.taskItemRepository = taskItemRepository;
  }

  public void ensureTasksCreatedToday(Long userId, Long goalId) {
    goalService.ownedGoal(userId, goalId);
    DayRange range = todayRange();
    long count =
        taskItemRepository.countByGoalIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            goalId, range.startInclusive(), range.endExclusive());
    if (count == 0) {
      throw new AppException(
          HttpStatus.BAD_REQUEST,
          "Сначала добавьте хотя бы одну задачу на сегодня. Без задач дня нельзя запускать фокус и отправлять отчет.");
    }
  }

  public List<TaskItem> todayTasksForGoal(Goal goal) {
    DayRange range = todayRange();
    return taskItemRepository
        .findByGoalIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtAsc(
            goal.getId(), range.startInclusive(), range.endExclusive());
  }

  private DayRange todayRange() {
    ZoneId zoneId = ZoneId.systemDefault();
    LocalDate today = LocalDate.now(zoneId);
    OffsetDateTime startInclusive = today.atStartOfDay(zoneId).toOffsetDateTime();
    OffsetDateTime endExclusive = today.plusDays(1).atStartOfDay(zoneId).toOffsetDateTime();
    return new DayRange(startInclusive, endExclusive);
  }

  private record DayRange(OffsetDateTime startInclusive, OffsetDateTime endExclusive) {}
}
