package com.pomodoro.app.service;

import com.pomodoro.app.dto.AiDtos;
import com.pomodoro.app.enums.AiVerdict;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.ai.mode", havingValue = "mock", matchIfMissing = true)
public class MockAiService implements AiService {
  private static final String TASK_BLOCK_START = "TODAY_TASKS_START";
  private static final String TASK_BLOCK_END = "TODAY_TASKS_END";
  private static final Set<String> COMPLETION_MARKERS =
      Set.of("сделал", "выполнил", "готово", "done", "completed", "завершил", "закрыл");

  private final StorageService storageService;

  public MockAiService(StorageService storageService) {
    this.storageService = storageService;
  }

  @Override
  public AiDtos.AnalyzeResult analyzeReportImage(
      byte[] imageBytes, String userComment, AiDtos.GoalContext goalContext) {
    List<String> todayTasks = extractTodayTasks(goalContext.description());
    if (todayTasks.isEmpty()) {
      return new AiDtos.AnalyzeResult(
          AiVerdict.NEEDS_MORE_INFO,
          0.45,
          "На сегодня не найден список задач для проверки. Добавьте задачи дня и повторите отчет.");
    }

    String normalizedComment = normalize(userComment);
    if (normalizedComment.isBlank()) {
      return new AiDtos.AnalyzeResult(
          AiVerdict.NEEDS_MORE_INFO,
          0.48,
          "Не указан комментарий к отчету. Напишите, какую задачу дня вы выполнили и какой результат получили.");
    }

    List<String> matchedTasks = matchTasks(todayTasks, normalizedComment);
    List<String> missingTasks =
        todayTasks.stream().filter(task -> !matchedTasks.contains(task)).toList();
    boolean hasCompletionMarker = containsAny(normalizedComment, COMPLETION_MARKERS);

    if (matchedTasks.isEmpty()) {
      return new AiDtos.AnalyzeResult(
          AiVerdict.REJECTED,
          0.78,
          "В комментарии не подтверждено выполнение задач дня. Ожидались задачи: "
              + previewTasks(todayTasks)
              + ".");
    }

    if (!hasCompletionMarker) {
      return new AiDtos.AnalyzeResult(
          AiVerdict.NEEDS_MORE_INFO,
          0.58,
          "Есть упоминание задач ("
              + previewTasks(matchedTasks)
              + "), но нет явного результата. Добавьте формулировку «выполнено/готово» и итог по фото.");
    }

    if (!missingTasks.isEmpty()) {
      return new AiDtos.AnalyzeResult(
          AiVerdict.NEEDS_MORE_INFO,
          0.66,
          "Подтверждены не все задачи дня. Подтверждено: "
              + previewTasks(matchedTasks)
              + ". Не хватает подтверждения по: "
              + previewTasks(missingTasks)
              + ".");
    }

    return new AiDtos.AnalyzeResult(
        AiVerdict.APPROVED,
        0.86,
        "Отчет подтверждает выполнение задач дня: " + previewTasks(matchedTasks) + ".");
  }

  @Override
  public String chat(List<AiDtos.ChatInputMessage> messages, AiDtos.GoalContext goalContext) {
    String last = messages.isEmpty() ? "" : messages.get(messages.size() - 1).content();
    String fullName =
        messages.stream()
            .filter(m -> "system".equalsIgnoreCase(m.role()))
            .map(AiDtos.ChatInputMessage::content)
            .map(this::extractFullName)
            .filter(name -> !name.isBlank())
            .findFirst()
            .orElse("пользователь");
    return "План на следующий шаг для цели '"
        + goalContext.title()
        + "' для "
        + fullName
        + ": 1) 25 минут фокуса 2) закрыть 1 задачу 3) отправить отчет с конкретным результатом. Вопрос: "
        + last;
  }

  @Override
  public AiDtos.ImageResult generateMotivationImage(
      AiDtos.GoalContext goalContext, String styleOptions) {
    String prompt =
        "Motivational image about goal: "
            + goalContext.title()
            + ". Style: "
            + (styleOptions == null ? "cinematic" : styleOptions);
    String svg =
        "<svg xmlns='http://www.w3.org/2000/svg' width='1024' height='1024'><rect width='100%' height='100%' fill='#f4efe6'/>"
            + "<text x='60' y='180' font-size='54' fill='#1f2937'>Pomodoro Vision</text>"
            + "<text x='60' y='260' font-size='36' fill='#b45309'>"
            + escape(goalContext.title())
            + "</text>"
            + "<text x='60' y='340' font-size='28' fill='#374151'>"
            + escape(styleOptions == null ? "Focus. Build. Finish." : styleOptions)
            + "</text>"
            + "</svg>";
    String path =
        storageService.storeBytes(svg.getBytes(StandardCharsets.UTF_8), "motivation", "svg");
    return new AiDtos.ImageResult(path, prompt);
  }

  private String escape(String text) {
    return text == null ? "" : text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
  }

  private String extractFullName(String systemPrompt) {
    String marker = "fullName:";
    int index = systemPrompt.indexOf(marker);
    if (index < 0) {
      return "";
    }
    String tail = systemPrompt.substring(index + marker.length()).trim();
    int lineBreak = tail.indexOf('\n');
    return (lineBreak >= 0 ? tail.substring(0, lineBreak) : tail).trim();
  }

  private List<String> extractTodayTasks(String description) {
    if (description == null || description.isBlank()) {
      return List.of();
    }
    int start = description.indexOf(TASK_BLOCK_START);
    int end = description.indexOf(TASK_BLOCK_END);
    if (start < 0 || end <= start) {
      return List.of();
    }
    String block = description.substring(start + TASK_BLOCK_START.length(), end);
    return block.lines()
        .map(String::trim)
        .filter(line -> line.startsWith("- "))
        .map(line -> line.substring(2).replaceAll("\\s*\\(статус:.*\\)$", "").trim())
        .filter(line -> !line.isBlank())
        .toList();
  }

  private List<String> matchTasks(List<String> tasks, String normalizedComment) {
    List<String> matched = new ArrayList<>();
    for (String task : tasks) {
      List<String> keywords = extractKeywords(task);
      if (keywords.isEmpty()) {
        continue;
      }
      boolean allPresent = keywords.stream().allMatch(normalizedComment::contains);
      boolean anyPresent = keywords.stream().anyMatch(normalizedComment::contains);
      if (allPresent || anyPresent) {
        matched.add(task);
      }
    }
    return matched;
  }

  private List<String> extractKeywords(String source) {
    String normalized = normalize(source);
    if (normalized.isBlank()) {
      return List.of();
    }
    return java.util.Arrays.stream(normalized.split(" "))
        .filter(token -> token.length() >= 4)
        .limit(3)
        .toList();
  }

  private String normalize(String text) {
    if (text == null) {
      return "";
    }
    return text.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{Nd}\\s]", " ").replaceAll("\\s+", " ").trim();
  }

  private boolean containsAny(String text, Set<String> markers) {
    return markers.stream().anyMatch(text::contains);
  }

  private String previewTasks(List<String> tasks) {
    return tasks.stream().limit(3).collect(Collectors.joining(", "));
  }
}
