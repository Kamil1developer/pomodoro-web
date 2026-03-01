package com.pomodoro.app.repository;

import com.pomodoro.app.entity.TaskItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskItemRepository extends JpaRepository<TaskItem, Long> {
  List<TaskItem> findByGoalIdOrderByCreatedAtDesc(Long goalId);

  Optional<TaskItem> findByIdAndGoalId(Long id, Long goalId);

  long countByGoalId(Long goalId);

  long countByGoalIdAndIsDoneTrue(Long goalId);
}
