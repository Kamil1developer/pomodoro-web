package com.pomodoro.app.repository;

import com.pomodoro.app.entity.FocusSession;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FocusSessionRepository extends JpaRepository<FocusSession, Long> {
  List<FocusSession> findByGoalIdOrderByStartedAtDesc(Long goalId);

  Optional<FocusSession> findFirstByGoalIdAndEndedAtIsNullOrderByStartedAtDesc(Long goalId);

  List<FocusSession> findByGoalIdAndStartedAtBetween(
      Long goalId, OffsetDateTime start, OffsetDateTime end);
}
