package com.pomodoro.app.repository;

import com.pomodoro.app.entity.GoalEvent;
import com.pomodoro.app.enums.GoalEventType;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoalEventRepository extends JpaRepository<GoalEvent, Long> {
  List<GoalEvent> findByGoalIdAndUserIdOrderByCreatedAtDesc(Long goalId, Long userId);

  List<GoalEvent> findTop20ByGoalIdAndUserIdOrderByCreatedAtDesc(Long goalId, Long userId);

  boolean existsByCommitmentIdAndTypeInAndNewValue(
      Long commitmentId, Collection<GoalEventType> types, String newValue);
}
