package com.pomodoro.app.service;

import com.pomodoro.app.dto.AiDtos;
import com.pomodoro.app.dto.ChatDtos;
import com.pomodoro.app.dto.GoalExperienceDtos;
import com.pomodoro.app.entity.ChatMessage;
import com.pomodoro.app.entity.ChatThread;
import com.pomodoro.app.entity.FocusSession;
import com.pomodoro.app.entity.Goal;
import com.pomodoro.app.entity.GoalCommitment;
import com.pomodoro.app.entity.MotivationImageFeedback;
import com.pomodoro.app.entity.Report;
import com.pomodoro.app.entity.TaskItem;
import com.pomodoro.app.entity.User;
import com.pomodoro.app.enums.ChatRole;
import com.pomodoro.app.enums.GoalStatus;
import com.pomodoro.app.repository.ChatMessageRepository;
import com.pomodoro.app.repository.ChatThreadRepository;
import com.pomodoro.app.repository.FocusSessionRepository;
import com.pomodoro.app.repository.GoalCommitmentRepository;
import com.pomodoro.app.repository.GoalEventRepository;
import com.pomodoro.app.repository.GoalRepository;
import com.pomodoro.app.repository.MotivationImageFeedbackRepository;
import com.pomodoro.app.repository.ReportRepository;
import com.pomodoro.app.repository.TaskItemRepository;
import com.pomodoro.app.repository.UserRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatService {
  private final GoalService goalService;
  private final GoalRepository goalRepository;
  private final GoalCommitmentRepository goalCommitmentRepository;
  private final TaskItemRepository taskItemRepository;
  private final UserRepository userRepository;
  private final ChatThreadRepository chatThreadRepository;
  private final ChatMessageRepository chatMessageRepository;
  private final FocusSessionRepository focusSessionRepository;
  private final ReportRepository reportRepository;
  private final GoalEventRepository goalEventRepository;
  private final MotivationImageFeedbackRepository motivationImageFeedbackRepository;
  private final GoalCommitmentService goalCommitmentService;
  private final AiService aiService;

  public ChatService(
      GoalService goalService,
      GoalRepository goalRepository,
      GoalCommitmentRepository goalCommitmentRepository,
      TaskItemRepository taskItemRepository,
      UserRepository userRepository,
      ChatThreadRepository chatThreadRepository,
      ChatMessageRepository chatMessageRepository,
      FocusSessionRepository focusSessionRepository,
      ReportRepository reportRepository,
      GoalEventRepository goalEventRepository,
      MotivationImageFeedbackRepository motivationImageFeedbackRepository,
      GoalCommitmentService goalCommitmentService,
      AiService aiService) {
    this.goalService = goalService;
    this.goalRepository = goalRepository;
    this.goalCommitmentRepository = goalCommitmentRepository;
    this.taskItemRepository = taskItemRepository;
    this.userRepository = userRepository;
    this.chatThreadRepository = chatThreadRepository;
    this.chatMessageRepository = chatMessageRepository;
    this.focusSessionRepository = focusSessionRepository;
    this.reportRepository = reportRepository;
    this.goalEventRepository = goalEventRepository;
    this.motivationImageFeedbackRepository = motivationImageFeedbackRepository;
    this.goalCommitmentService = goalCommitmentService;
    this.aiService = aiService;
  }

  @Transactional
  public ChatDtos.ChatHistoryResponse send(Long userId, Long goalId, String content) {
    Goal goal = goalService.ownedGoal(userId, goalId);
    ChatThread thread = latestOrCreateThread(goal);

    chatMessageRepository.save(
        ChatMessage.builder()
            .thread(thread)
            .role(ChatRole.USER)
            .content(content)
            .createdAt(OffsetDateTime.now())
            .build());

    List<ChatMessage> history = chatMessageRepository.findByThreadIdOrderByCreatedAtAsc(thread.getId());
    List<AiDtos.ChatInputMessage> aiMessages = new ArrayList<>();
    aiMessages.add(new AiDtos.ChatInputMessage("system", buildUserContextPrompt(userId, goal, content, history)));
    history.forEach(
        message ->
            aiMessages.add(
                new AiDtos.ChatInputMessage(
                    message.getRole().name().toLowerCase(Locale.ROOT), message.getContent())));

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

  @Transactional(readOnly = true)
  public ChatDtos.ChatHistoryResponse history(Long userId, Long goalId) {
    Goal goal = goalService.ownedGoal(userId, goalId);
    ChatThread thread = latestOrCreateThread(goal);
    return history(thread.getId());
  }

  @Transactional
  public ChatDtos.ChatHistoryResponse clearHistory(Long userId, Long goalId) {
    Goal goal = goalService.ownedGoal(userId, goalId);
    List<ChatThread> threads = chatThreadRepository.findByGoalIdOrderByCreatedAtDesc(goalId);
    for (ChatThread thread : threads) {
      chatMessageRepository.deleteByThreadId(thread.getId());
    }
    chatThreadRepository.deleteAll(threads);
    ChatThread freshThread = createThread(goal);
    return history(freshThread.getId());
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

  private ChatThread latestOrCreateThread(Goal goal) {
    return chatThreadRepository
        .findFirstByGoalIdOrderByCreatedAtDesc(goal.getId())
        .orElseGet(() -> createThread(goal));
  }

  private ChatThread createThread(Goal goal) {
    return chatThreadRepository.save(
        ChatThread.builder().goal(goal).createdAt(OffsetDateTime.now()).build());
  }

  private String buildUserContextPrompt(
      Long userId, Goal activeGoal, String lastUserMessage, List<ChatMessage> history) {
    User user = userRepository.findById(userId).orElseThrow();
    GoalExperienceDtos.TodayStatusResponse today = goalCommitmentService.getTodayStatus(userId, activeGoal.getId());
    GoalExperienceDtos.ForecastResponse forecast = goalCommitmentService.getForecast(userId, activeGoal.getId());
    List<Goal> goals = goalRepository.findByUserIdOrderByCreatedAtDesc(userId);
    Map<Long, List<TaskItem>> tasksByGoal =
        taskItemRepository.findByGoalUserIdOrderByGoalIdAscCreatedAtAsc(userId).stream()
            .collect(Collectors.groupingBy(task -> task.getGoal().getId()));
    Map<Long, GoalCommitment> commitments =
        goalCommitmentRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
            .collect(
                Collectors.toMap(
                    commitment -> commitment.getGoal().getId(),
                    commitment -> commitment,
                    (left, right) -> left));

    List<FocusSession> recentSessions =
        focusSessionRepository.findByGoalIdOrderByStartedAtDesc(activeGoal.getId()).stream().limit(5).toList();
    List<Report> recentReports = reportRepository.findByGoalIdOrderByCreatedAtDesc(activeGoal.getId()).stream().limit(5).toList();
    List<MotivationImageFeedback> feedbacks = motivationImageFeedbackRepository.findByUserId(userId);
    long hiddenCount = feedbacks.stream().filter(feedback -> feedback.getType().name().equals("NOT_INTERESTED")).count();
    long reportedCount = feedbacks.stream().filter(feedback -> feedback.getType().name().equals("REPORTED")).count();

    String goalsContext =
        goals.stream()
            .map(
                goal -> {
                  List<TaskItem> tasks = tasksByGoal.getOrDefault(goal.getId(), List.of());
                  GoalCommitment commitment = commitments.get(goal.getId());
                  long done = tasks.stream().filter(TaskItem::getIsDone).count();
                  return "- ["
                      + goal.getStatus().name()
                      + "] "
                      + goal.getTitle()
                      + " | streak="
                      + goal.getCurrentStreak()
                      + " | tasksDone="
                      + done
                      + "/"
                      + tasks.size()
                      + " | deadline="
                      + (goal.getDeadline() == null ? "-" : goal.getDeadline())
                      + " | discipline="
                      + (commitment == null ? "-" : commitment.getDisciplineScore())
                      + " | risk="
                      + (commitment == null ? "-" : commitment.getRiskStatus())
                      + (goal.getFailureReason() == null ? "" : " | failureReason=" + goal.getFailureReason());
                })
            .collect(Collectors.joining("\n"));

    String sessionsContext =
        recentSessions.stream()
            .map(
                session ->
                    "- "
                        + session.getStartedAt()
                        + " | duration="
                        + (session.getDurationMinutes() == null ? "active" : session.getDurationMinutes() + " min"))
            .collect(Collectors.joining("\n"));

    String reportsContext =
        recentReports.stream()
            .map(
                report ->
                    "- "
                        + report.getReportDate()
                        + " | status="
                        + report.getStatus()
                        + " | verdict="
                        + report.getAiVerdict()
                        + " | explanation="
                        + (report.getAiExplanation() == null ? "-" : report.getAiExplanation()))
            .collect(Collectors.joining("\n"));

    String eventsContext =
        goalEventRepository.findTop20ByGoalIdAndUserIdOrderByCreatedAtDesc(activeGoal.getId(), userId).stream()
            .limit(6)
            .map(event -> "- " + event.getCreatedAt() + " | " + event.getTitle() + " | " + (event.getDescription() == null ? "-" : event.getDescription()))
            .collect(Collectors.joining("\n"));

    String recentMessages =
        history.stream()
            .sorted(Comparator.comparing(ChatMessage::getCreatedAt).reversed())
            .limit(6)
            .map(message -> message.getRole() + ": " + message.getContent())
            .collect(Collectors.joining("\n"));

    return "Ты — Мотиватор в Pomodoro Web. Ты не просто говоришь 'сделай Pomodoro', а ведёшь пользователя к цели с учётом его реального состояния."
        + "\nОтвечай ТОЛЬКО на русском языке, тепло, без осуждения и без шаблонной канцелярии."
        + "\nМожно помогать с мотивацией, планированием дня, борьбой с прокрастинацией, концентрацией, привычками, обучением и мягкой поддержкой."
        + "\nЕсли пользователь отстаёт — предложи короткий следующий шаг. Если идёт хорошо — поддержи и помоги не перегрузиться."
        + "\nЕсли риск высокий — предложи уменьшить давление и начать с маленькой сессии. Если отчёта нет — напомни про фото-отчёт."
        + "\nФормат ответа: 1) короткий разбор ситуации, 2) следующий конкретный шаг на 15-30 минут, 3) при необходимости мини-план на остаток дня."
        + "\nПользователь:"
        + "\n- id: "
        + user.getId()
        + "\n- fullName: "
        + user.getFullName()
        + "\n- email: "
        + user.getEmail()
        + "\n- avatarPath: "
        + (user.getAvatarPath() == null ? "-" : user.getAvatarPath())
        + "\nАктивная цель:"
        + "\n- title: "
        + activeGoal.getTitle()
        + "\n- description: "
        + (activeGoal.getDescription() == null ? "-" : activeGoal.getDescription())
        + "\n- targetHours: "
        + (activeGoal.getTargetHours() == null ? "-" : activeGoal.getTargetHours())
        + "\n- deadline: "
        + (activeGoal.getDeadline() == null ? "-" : activeGoal.getDeadline())
        + "\nСегодня по активной цели:"
        + "\n- dailyTargetMinutes: "
        + today.dailyTargetMinutes()
        + "\n- completedFocusMinutesToday: "
        + today.completedFocusMinutesToday()
        + "\n- remainingMinutesToday: "
        + today.remainingMinutesToday()
        + "\n- reportStatusToday: "
        + today.reportStatusToday()
        + "\n- hasApprovedReportToday: "
        + today.hasApprovedReportToday()
        + "\n- currentStreak: "
        + today.currentStreak()
        + "\n- disciplineScore: "
        + today.disciplineScore()
        + "\n- riskStatus: "
        + today.riskStatus()
        + "\n- nextRecommendedAction: "
        + today.nextRecommendedAction()
        + "\nПрогноз:"
        + "\n- probabilityLabel: "
        + forecast.probabilityLabel()
        + "\n- estimatedCompletionDate: "
        + forecast.estimatedCompletionDate()
        + "\n- onTrack: "
        + forecast.onTrack()
        + "\n- explanation: "
        + forecast.explanation()
        + "\nПоследние фокус-сессии:\n"
        + (sessionsContext.isBlank() ? "- нет данных" : sessionsContext)
        + "\nПоследние отчёты:\n"
        + (reportsContext.isBlank() ? "- нет данных" : reportsContext)
        + "\nПоследние события цели:\n"
        + (eventsContext.isBlank() ? "- нет данных" : eventsContext)
        + "\nВсе цели пользователя:\n"
        + (goalsContext.isBlank() ? "- нет других целей" : goalsContext)
        + "\nИстория последних сообщений:\n"
        + (recentMessages.isBlank() ? "- чат пуст" : recentMessages)
        + "\nПредпочтения мотивации: скрыто изображений="
        + hiddenCount
        + ", пожаловано изображений="
        + reportedCount
        + "\nТекущая дата: "
        + LocalDate.now(ZoneId.systemDefault())
        + "\nПоследний запрос пользователя: "
        + lastUserMessage;
  }
}
