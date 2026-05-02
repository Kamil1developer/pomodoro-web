package com.pomodoro.app.repository;

import com.pomodoro.app.entity.GoalCommitment;
import com.pomodoro.app.enums.CommitmentStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoalCommitmentRepository extends JpaRepository<GoalCommitment, Long> {
  Optional<GoalCommitment> findByGoalIdAndUserIdAndStatus(
      Long goalId, Long userId, CommitmentStatus status);

  Optional<GoalCommitment> findFirstByGoalIdAndUserIdOrderByCreatedAtDesc(Long goalId, Long userId);

  List<GoalCommitment> findByUserIdOrderByCreatedAtDesc(Long userId);

  List<GoalCommitment> findByStatus(CommitmentStatus status);
}
