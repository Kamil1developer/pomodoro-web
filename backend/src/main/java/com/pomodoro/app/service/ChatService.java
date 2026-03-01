package com.pomodoro.app.service;

import com.pomodoro.app.dto.AiDtos;
import com.pomodoro.app.dto.ChatDtos;
import com.pomodoro.app.entity.ChatMessage;
import com.pomodoro.app.entity.ChatThread;
import com.pomodoro.app.entity.Goal;
import com.pomodoro.app.entity.TaskItem;
import com.pomodoro.app.entity.User;
import com.pomodoro.app.enums.ChatRole;
import com.pomodoro.app.repository.ChatMessageRepository;
import com.pomodoro.app.repository.ChatThreadRepository;
import com.pomodoro.app.repository.GoalRepository;
import com.pomodoro.app.repository.TaskItemRepository;
import com.pomodoro.app.repository.UserRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ChatService {
  private final GoalService goalService;
  private final GoalRepository goalRepository;
  private final TaskItemRepository taskItemRepository;
  private final UserRepository userRepository;
  private final ChatThreadRepository chatThreadRepository;
  private final ChatMessageRepository chatMessageRepository;
  private final AiService aiService;

  public ChatService(
      GoalService goalService,
      GoalRepository goalRepository,
      TaskItemRepository taskItemRepository,
      UserRepository userRepository,
      ChatThreadRepository chatThreadRepository,
      ChatMessageRepository chatMessageRepository,
      AiService aiService) {
    this.goalService = goalService;
    this.goalRepository = goalRepository;
    this.taskItemRepository = taskItemRepository;
    this.userRepository = userRepository;
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
    aiMessages.add(
        new AiDtos.ChatInputMessage(
            "system", buildUserContextPrompt(userId, goal, content)));
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

  private String buildUserContextPrompt(Long userId, Goal activeGoal, String lastUserMessage) {
    User user = userRepository.findById(userId).orElseThrow();
    List<Goal> goals = goalRepository.findByUserIdOrderByCreatedAtDesc(userId);
    Map<Long, List<TaskItem>> tasksByGoal =
        taskItemRepository.findByGoalUserIdOrderByGoalIdAscCreatedAtAsc(userId).stream()
            .collect(Collectors.groupingBy(task -> task.getGoal().getId()));

    StringBuilder goalsContext = new StringBuilder();
    for (Goal goal : goals) {
      List<TaskItem> tasks = tasksByGoal.getOrDefault(goal.getId(), List.of());
      long done = tasks.stream().filter(TaskItem::getIsDone).count();
      goalsContext
          .append("\n- [")
          .append(goal.getId())
          .append("] ")
          .append(goal.getTitle())
          .append(" | streak=")
          .append(goal.getCurrentStreak())
          .append(" | tasksDone=")
          .append(done)
          .append("/")
          .append(tasks.size())
          .append(" | deadline=")
          .append(goal.getDeadline() == null ? "-" : goal.getDeadline())
          .append(" | color=")
          .append(goal.getThemeColor())
          .append("\n  tasks:");

      if (tasks.isEmpty()) {
        goalsContext.append(" none");
      } else {
        for (TaskItem task : tasks) {
          goalsContext
              .append("\n  - ")
              .append(Boolean.TRUE.equals(task.getIsDone()) ? "[x] " : "[ ] ")
              .append(task.getTitle());
        }
      }
    }

    return "Ты AI-коуч Pomodoro Web. Отвечай только на русском, кратко и практично."
        + "\nТекущий пользователь:"
        + "\n- id: "
        + user.getId()
        + "\n- fullName: "
        + user.getFullName()
        + "\n- email: "
        + user.getEmail()
        + "\n- role: "
        + user.getRole().name()
        + "\n- createdAt: "
        + user.getCreatedAt()
        + "\nАктивная цель:"
        + "\n- id: "
        + activeGoal.getId()
        + "\n- title: "
        + activeGoal.getTitle()
        + "\n- description: "
        + (activeGoal.getDescription() == null ? "-" : activeGoal.getDescription())
        + "\nВсе цели и задачи пользователя:"
        + goalsContext
        + "\nПоследний запрос пользователя: "
        + lastUserMessage
        + "\nТребования к ответу: 1) следующий шаг на 25-30 минут, 2) план на день (3 пункта), 3) что улучшить в отчете.";
  }
}
