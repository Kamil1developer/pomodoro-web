package com.pomodoro.app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pomodoro.app.config.AppProperties;
import com.pomodoro.app.dto.AiDtos;
import com.pomodoro.app.enums.AiVerdict;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@ConditionalOnProperty(name = "app.ai.mode", havingValue = "local")
public class LocalAiService implements AiService {
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final AppProperties appProperties;
  private final WebClient ollamaWebClient;
  private final WebClient localImageWebClient;
  private final StorageService storageService;

  public LocalAiService(
      AppProperties appProperties,
      WebClient.Builder webClientBuilder,
      StorageService storageService) {
    this.appProperties = appProperties;
    this.ollamaWebClient = webClientBuilder.baseUrl(appProperties.ai().ollamaApiUrl()).build();
    this.localImageWebClient =
        webClientBuilder.baseUrl(appProperties.ai().localImageApiUrl()).build();
    this.storageService = storageService;
  }

  @Override
  public AiDtos.AnalyzeResult analyzeReportImage(
      byte[] imageBytes, String userComment, AiDtos.GoalContext goalContext) {
    String comment = userComment == null ? "" : userComment.toLowerCase(Locale.ROOT);

    if (comment.contains("done")
        || comment.contains("completed")
        || comment.contains("сделал")
        || comment.contains("готово")) {
      return new AiDtos.AnalyzeResult(
          AiVerdict.APPROVED,
          0.74,
          "Локальный режим: по комментарию виден результат. Добавьте больше деталей для повышения точности.");
    }

    if (comment.isBlank()) {
      return new AiDtos.AnalyzeResult(
          AiVerdict.NEEDS_MORE_INFO,
          0.42,
          "Локальный режим: добавьте комментарий с конкретным выполненным шагом.");
    }

    return new AiDtos.AnalyzeResult(
        AiVerdict.REJECTED,
        0.63,
        "Локальный режим: недостаточно явных признаков завершения. Уточните итог и прикрепите более информативное фото.");
  }

  @Override
  public String chat(List<AiDtos.ChatInputMessage> messages, AiDtos.GoalContext goalContext) {
    StringBuilder conversation = new StringBuilder();
    StringBuilder systemContext = new StringBuilder();
    int start = Math.max(messages.size() - 10, 0);
    for (int i = start; i < messages.size(); i++) {
      AiDtos.ChatInputMessage message = messages.get(i);
      if ("system".equalsIgnoreCase(message.role())) {
        systemContext.append(message.content()).append("\n");
        continue;
      }
      conversation
          .append(message.role().toUpperCase(Locale.ROOT))
          .append(": ")
          .append(message.content())
          .append("\n");
    }

    String prompt =
        "Ты продуктивный коуч Pomodoro."
            + "\nЦель: "
            + goalContext.title()
            + "\nОписание цели: "
            + (goalContext.description() == null ? "-" : goalContext.description())
            + "\nКонтекст:\n"
            + systemContext
            + "\nИстория диалога:\n"
            + conversation
            + "\nДай короткий и практичный ответ на русском: следующий шаг, план на 1 день и как улучшить отчет.";

    try {
      String response =
          ollamaWebClient
              .post()
              .uri("/api/generate")
              .contentType(MediaType.APPLICATION_JSON)
              .bodyValue(
                  Map.of(
                      "model", appProperties.ai().ollamaModel(), "prompt", prompt, "stream", false))
              .retrieve()
              .bodyToMono(String.class)
              .block();

      JsonNode root = objectMapper.readTree(response);
      String text = root.path("response").asText();
      if (text == null || text.isBlank()) {
        return "Локальная модель не вернула ответ. Попробуйте еще раз.";
      }
      return text.trim();
    } catch (Exception e) {
      return "Локальный чат недоступен. Убедитесь, что сервис Ollama запущен и модель загружена.";
    }
  }

  @Override
  public AiDtos.ImageResult generateMotivationImage(
      AiDtos.GoalContext goalContext, String styleOptions) {
    String prompt =
        "Motivational result visualization for goal: "
            + goalContext.title()
            + ". Description: "
            + (goalContext.description() == null ? "-" : goalContext.description())
            + ". Style: "
            + (styleOptions == null || styleOptions.isBlank() ? "cinematic" : styleOptions)
            + ". high quality, inspirational atmosphere.";

    try {
      String response =
          localImageWebClient
              .post()
              .uri("/generate")
              .contentType(MediaType.APPLICATION_JSON)
              .bodyValue(
                  Map.of(
                      "prompt",
                      prompt,
                      "num_inference_steps",
                      appProperties.ai().localImageSteps(),
                      "guidance_scale",
                      0.0,
                      "width",
                      256,
                      "height",
                      256))
              .retrieve()
              .bodyToMono(String.class)
              .block();

      JsonNode root = objectMapper.readTree(response);
      String b64 = root.path("b64_image").asText();
      String path = storageService.storeMotivationBase64(b64, "png");
      return new AiDtos.ImageResult(path, prompt);
    } catch (Exception e) {
      String svg =
          "<svg xmlns='http://www.w3.org/2000/svg' width='1024' height='1024'><rect width='100%' height='100%' fill='#f8f7f2'/>"
              + "<text x='70' y='210' font-size='58' fill='#1d4f47'>Local Image AI Unavailable</text>"
              + "<text x='70' y='300' font-size='32' fill='#374151'>"
              + escape(goalContext.title())
              + "</text>"
              + "<text x='70' y='370' font-size='26' fill='#6b7280'>"
              + escape(styleOptions == null ? "cinematic" : styleOptions)
              + "</text></svg>";
      String fallbackPath =
          storageService.storeBytes(svg.getBytes(StandardCharsets.UTF_8), "motivation", "svg");
      return new AiDtos.ImageResult(fallbackPath, prompt);
    }
  }

  private String escape(String text) {
    return text == null ? "" : text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
  }
}
