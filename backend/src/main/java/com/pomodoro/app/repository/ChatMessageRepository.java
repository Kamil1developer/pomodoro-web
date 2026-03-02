package com.pomodoro.app.repository;

import com.pomodoro.app.entity.ChatMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
  List<ChatMessage> findByThreadIdOrderByCreatedAtAsc(Long threadId);

  void deleteByThreadId(Long threadId);
}
