package com.pomodoro.app.dto;

import com.pomodoro.app.enums.ChatRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;

public class ChatDtos {
  public record ChatSendRequest(@NotBlank @Size(max = 4000) String content) {}

  public record ChatMessageResponse(
      Long id, ChatRole role, String content, OffsetDateTime createdAt) {}

  public record ChatHistoryResponse(Long threadId, List<ChatMessageResponse> messages) {}
}
