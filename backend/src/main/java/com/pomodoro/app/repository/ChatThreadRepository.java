package com.pomodoro.app.repository;

import com.pomodoro.app.entity.ChatThread;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatThreadRepository extends JpaRepository<ChatThread, Long> {
  Optional<ChatThread> findFirstByGoalIdOrderByCreatedAtDesc(Long goalId);
}
