package com.pomodoro.app.service;

import com.pomodoro.app.dto.AiDtos;
import com.pomodoro.app.dto.ChatDtos;
import com.pomodoro.app.entity.ChatMessage;
import com.pomodoro.app.entity.ChatThread;
import com.pomodoro.app.entity.Goal;
import com.pomodoro.app.enums.ChatRole;
import com.pomodoro.app.repository.ChatMessageRepository;
import com.pomodoro.app.repository.ChatThreadRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ChatService {
  private final GoalService goalService;
  private final ChatThreadRepository chatThreadRepository;
  private final ChatMessageRepository chatMessageRepository;
  private final AiService aiService;

  public ChatService(
      GoalService goalService,
      ChatThreadRepository chatThreadRepository,
      ChatMessageRepository chatMessageRepository,
      AiService aiService) {
    this.goalService = goalService;
    this.chatThreadRepository = chatThreadRepository;
    this.chatMessageRepository = chatMessageRepository;
    this.aiService = aiService;
  }

  public ChatDtos.ChatHistoryResponse send(Long userId, Long goalId, String content) {
    Goal goal = goalService.ownedGoal(userId, goalId);
    ChatThread thread =
        chatThreadRepository
            .findFirstByGoalIdOrderByCreatedAtDesc(goalId)
            .orElseGet(
                () ->
                    chatThreadRepository.save(
                        ChatThread.builder().goal(goal).createdAt(OffsetDateTime.now()).build()));

    chatMessageRepository.save(
        ChatMessage.builder()
            .thread(thread)
            .role(ChatRole.USER)
            .content(content)
            .createdAt(OffsetDateTime.now())
            .build());

    List<ChatMessage> history =
        chatMessageRepository.findByThreadIdOrderByCreatedAtAsc(thread.getId());
    List<AiDtos.ChatInputMessage> aiMessages = new ArrayList<>();
    aiMessages.add(new AiDtos.ChatInputMessage("system", "You are a pomodoro productivity coach."));
    history.forEach(
        m ->
            aiMessages.add(
                new AiDtos.ChatInputMessage(m.getRole().name().toLowerCase(), m.getContent())));

    String assistant =
        aiService.chat(
            aiMessages,
            new AiDtos.GoalContext(goal.getId(), goal.getTitle(), goal.getDescription()));

    chatMessageRepository.save(
        ChatMessage.builder()
            .thread(thread)
            .role(ChatRole.ASSISTANT)
            .content(assistant)
            .createdAt(OffsetDateTime.now())
            .build());

    return history(thread.getId());
  }

  public ChatDtos.ChatHistoryResponse history(Long userId, Long goalId) {
    goalService.ownedGoal(userId, goalId);
    ChatThread thread =
        chatThreadRepository
            .findFirstByGoalIdOrderByCreatedAtDesc(goalId)
            .orElseGet(
                () ->
                    chatThreadRepository.save(
                        ChatThread.builder()
                            .goal(goalService.ownedGoal(userId, goalId))
                            .createdAt(OffsetDateTime.now())
                            .build()));
    return history(thread.getId());
  }

  private ChatDtos.ChatHistoryResponse history(Long threadId) {
    List<ChatDtos.ChatMessageResponse> messages =
        chatMessageRepository.findByThreadIdOrderByCreatedAtAsc(threadId).stream()
            .map(
                m ->
                    new ChatDtos.ChatMessageResponse(
                        m.getId(), m.getRole(), m.getContent(), m.getCreatedAt()))
            .toList();
    return new ChatDtos.ChatHistoryResponse(threadId, messages);
  }
}
