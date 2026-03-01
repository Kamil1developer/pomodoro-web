package com.pomodoro.app.service;

import com.pomodoro.app.dto.AiDtos;
import com.pomodoro.app.enums.AiVerdict;
import java.nio.charset.StandardCharsets;
import java.util.List;
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
    if (userComment != null && userComment.toLowerCase().contains("done")) {
      return new AiDtos.AnalyzeResult(
          AiVerdict.APPROVED,
          0.88,
          "Комментарий указывает на завершение этапа и фото выглядит релевантным.");
    }
    if (userComment != null && userComment.toLowerCase().contains("later")) {
      return new AiDtos.AnalyzeResult(
          AiVerdict.NEEDS_MORE_INFO,
          0.51,
          "Нужно больше деталей: добавьте конкретный результат по задаче и четкое фото прогресса.");
    }
    return new AiDtos.AnalyzeResult(
        AiVerdict.REJECTED,
        0.79,
        "Недостаточно подтверждений выполнения. Добавьте фото конечного результата и уточните что завершено.");
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
}
