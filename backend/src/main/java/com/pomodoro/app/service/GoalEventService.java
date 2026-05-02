package com.pomodoro.app.service;

import com.pomodoro.app.dto.GoalExperienceDtos;
import com.pomodoro.app.entity.Goal;
import com.pomodoro.app.entity.GoalCommitment;
import com.pomodoro.app.entity.GoalEvent;
import com.pomodoro.app.enums.GoalEventType;
import com.pomodoro.app.repository.GoalEventRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GoalEventService {
  private final GoalEventRepository goalEventRepository;

  public GoalEventService(GoalEventRepository goalEventRepository) {
    this.goalEventRepository = goalEventRepository;
  }

  @Transactional
  public GoalEvent createEvent(
      Goal goal,
      GoalCommitment commitment,
      GoalEventType type,
      String title,
      String description,
      String oldValue,
      String newValue) {
    return createEvent(
        goal, commitment, type, title, description, oldValue, newValue, OffsetDateTime.now());
  }

  @Transactional
  public GoalEvent createEvent(
      Goal goal,
      GoalCommitment commitment,
      GoalEventType type,
      String title,
      String description,
      String oldValue,
      String newValue,
      OffsetDateTime createdAt) {
    GoalEvent event = new GoalEvent();
    event.setUser(goal.getUser());
    event.setGoal(goal);
    event.setCommitment(commitment);
    event.setType(type);
    event.setTitle(title);
    event.setDescription(description);
    event.setOldValue(oldValue);
    event.setNewValue(newValue);
    event.setCreatedAt(createdAt);
    return goalEventRepository.save(event);
  }

  @Transactional(readOnly = true)
  public List<GoalExperienceDtos.GoalEventResponse> getRecentEvents(Long goalId, Long userId) {
    return goalEventRepository
        .findTop20ByGoalIdAndUserIdOrderByCreatedAtDesc(goalId, userId)
        .stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public boolean hasProcessedDay(Long commitmentId, String dateKey) {
    return goalEventRepository.existsByCommitmentIdAndTypeInAndNewValue(
        commitmentId, List.of(GoalEventType.DAY_COMPLETED, GoalEventType.DAY_MISSED), dateKey);
  }

  public GoalExperienceDtos.GoalEventResponse toResponse(GoalEvent event) {
    return new GoalExperienceDtos.GoalEventResponse(
        event.getId(),
        event.getGoal().getId(),
        event.getType(),
        event.getTitle(),
        event.getDescription(),
        event.getOldValue(),
        event.getNewValue(),
        event.getCreatedAt());
  }
}
