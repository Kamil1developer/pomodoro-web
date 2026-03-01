package com.pomodoro.app.controller;

import com.pomodoro.app.dto.ChatDtos;
import com.pomodoro.app.service.ChatService;
import com.pomodoro.app.util.AuthUtil;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/goals/{goalId}/chat")
public class ChatController {
  private final ChatService chatService;

  public ChatController(ChatService chatService) {
    this.chatService = chatService;
  }

  @PostMapping("/send")
  public ChatDtos.ChatHistoryResponse send(
      @PathVariable Long goalId, @RequestBody @Valid ChatDtos.ChatSendRequest request) {
    return chatService.send(AuthUtil.currentUserId(), goalId, request.content());
  }

  @GetMapping("/history")
  public ChatDtos.ChatHistoryResponse history(@PathVariable Long goalId) {
    return chatService.history(AuthUtil.currentUserId(), goalId);
  }
}
