package com.pomodoro.app.repository;

import com.pomodoro.app.entity.Goal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoalRepository extends JpaRepository<Goal, Long> {
  List<Goal> findByUserIdOrderByCreatedAtDesc(Long userId);

  Optional<Goal> findByIdAndUserId(Long id, Long userId);
}
