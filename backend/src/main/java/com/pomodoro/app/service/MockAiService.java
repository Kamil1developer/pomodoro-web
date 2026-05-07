package com.pomodoro.app.service;

import com.pomodoro.app.dto.AiDtos;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.ai.mode", havingValue = "mock", matchIfMissing = true)
public class MockAiService implements AiService {
  private final StorageService storageService;

  public MockAiService(StorageService storageService) {
    this.storageService = storageService;
  }

  @Override
  public AiDtos.AnalyzeResult analyzeReportImage(
      byte[] imageBytes, String userComment, AiDtos.GoalContext goalContext) {
    return ReportEvidenceRules.evaluate(userComment, goalContext);
  }

  @Override
  public String chat(List<AiDtos.ChatInputMessage> messages, AiDtos.GoalContext goalContext) {
    String systemPrompt =
        messages.stream()
            .filter(message -> "system".equalsIgnoreCase(message.role()))
            .map(AiDtos.ChatInputMessage::content)
            .findFirst()
            .orElse("");
    String userText =
        messages.stream()
            .filter(message -> "user".equalsIgnoreCase(message.role()))
            .reduce((left, right) -> right)
            .map(AiDtos.ChatInputMessage::content)
            .orElse("")
            .toLowerCase(Locale.ROOT);

    String remaining = extractValue(systemPrompt, "remainingMinutesToday:");
    String streak = extractValue(systemPrompt, "currentStreak:");
    String discipline = extractValue(systemPrompt, "disciplineScore:");
    String risk = extractValue(systemPrompt, "riskStatus:");
    String reportStatus = extractValue(systemPrompt, "reportStatusToday:");
    String nextAction = extractValue(systemPrompt, "nextRecommendedAction:");
    String walletBalance = extractValue(systemPrompt, "walletBalance:");
    String dailyPenalty = extractValue(systemPrompt, "dailyPenaltyAmount:");
    String penaltyWarning = extractValue(systemPrompt, "nextPenaltyWarning:");
    String fullName = extractValue(systemPrompt, "fullName:");
    String greeting = fullName.isBlank() ? "Смотрите" : fullName + ", смотрите";

    if (containsAny(userText, "деньги", "баланс", "штраф", "монет")) {
      return greeting
          + ": виртуальный баланс сейчас "
          + safeValue(walletBalance, "не рассчитан")
          + " монет, штраф за пропуск — "
          + safeValue(dailyPenalty, "0")
          + " монет."
          + "\nЧтобы не потерять деньги сегодня: 1) закройте минимум одну короткую сессию, 2) доведите дневную норму до нуля, 3) отправьте фото-отчёт с доказательством результата."
          + "\nПодсказка системы: "
          + safeValue(
              penaltyWarning, "если день выполнить и подтвердить отчётом, штраф не спишется.");
    }

    if (containsAny(userText, "что мне сделать сегодня", "план", "вечер")) {
      return greeting
          + ": сегодня у вас осталось "
          + safeValue(remaining, "немного")
          + " до дневной нормы."
          + "\n1. Следующий шаг: одна короткая фокус-сессия на 20–25 минут."
          + "\n2. Потом закройте одну конкретную задачу по цели «"
          + goalContext.title()
          + "»."
          + "\n3. В конце отправьте фото-отчёт с результатом, а не только с процессом.";
    }

    if (containsAny(userText, "почему я отстаю", "отстаю", "не успеваю", "прокраст")) {
      return greeting
          + ": похоже, темп сбился не потому, что вы ленивы, а потому что вход в задачу стал слишком тяжёлым."
          + "\nСейчас важнее не догонять весь план, а сделать один маленький доказуемый шаг."
          + "\nНачните с 15 минут по самой простой части цели, затем вернитесь к шагу: "
          + safeValue(nextAction, "сделайте короткую фокус-сессию и зафиксируйте результат")
          + ".";
    }

    if (containsAny(userText, "мотивац", "как начать", "не хочу", "нет сил")) {
      return greeting
          + ": мотивация обычно приходит после действия, а не до него."
          + "\nНе требуйте от себя большого рывка: откройте задачу по цели «"
          + goalContext.title()
          + "», поработайте 10–15 минут и остановитесь только после маленького результата."
          + "\nСерия сейчас: "
          + safeValue(streak, "0")
          + " дн., дисциплина: "
          + safeValue(discipline, "не рассчитана")
          + ". Даже короткая сессия сегодня удержит темп.";
    }

    if ("HIGH".equalsIgnoreCase(risk)) {
      return greeting
          + ": цель сейчас в зоне риска, поэтому давить на себя сильнее не нужно."
          + "\nСнизьте ожидание на сегодня до одного короткого блока, затем проверьте отчёт и только после этого решайте, нужен ли второй заход."
          + "\nОриентир: "
          + safeValue(nextAction, "сделайте маленький шаг и закрепите его отчётом")
          + ".";
    }

    if (reportStatus == null || reportStatus.isBlank() || "null".equalsIgnoreCase(reportStatus)) {
      return greeting
          + ": по цели уже есть движение, но день не будет засчитан без подтверждения результата."
          + "\nПосле следующего шага подготовьте фото-отчёт так, чтобы на нём был виден именно результат: код, конспект, документ, выполненное упражнение или другая практическая часть.";
    }

    return greeting
        + ": у вас уже есть опора для движения по цели «"
        + goalContext.title()
        + "»."
        + "\nСледующий шаг: "
        + safeValue(nextAction, "одна короткая Pomodoro-сессия")
        + "."
        + "\nЕсли после неё останутся силы, добавьте ещё один маленький результат и сразу зафиксируйте его отчётом.";
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

  private boolean containsAny(String text, String... needles) {
    for (String needle : needles) {
      if (text.contains(needle)) {
        return true;
      }
    }
    return false;
  }

  private String extractValue(String systemPrompt, String marker) {
    int index = systemPrompt.indexOf(marker);
    if (index < 0) {
      return "";
    }
    String tail = systemPrompt.substring(index + marker.length()).trim();
    int lineBreak = tail.indexOf('\n');
    return (lineBreak >= 0 ? tail.substring(0, lineBreak) : tail).trim();
  }

  private String safeValue(String value, String fallback) {
    return value == null || value.isBlank() || "null".equalsIgnoreCase(value) ? fallback : value;
  }

  private String escape(String text) {
    return text == null ? "" : text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
  }
}
