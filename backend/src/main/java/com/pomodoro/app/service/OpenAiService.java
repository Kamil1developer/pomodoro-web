package com.pomodoro.app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pomodoro.app.config.AppProperties;
import com.pomodoro.app.dto.AiDtos;
import com.pomodoro.app.enums.AiVerdict;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@ConditionalOnProperty(name = "app.ai.mode", havingValue = "openai")
public class OpenAiService implements AiService {
  private final AppProperties appProperties;
  private final WebClient webClient;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final StorageService storageService;

  public OpenAiService(
      AppProperties appProperties, WebClient openAiWebClient, StorageService storageService) {
    this.appProperties = appProperties;
    this.webClient = openAiWebClient;
    this.storageService = storageService;
  }

  @Override
  public AiDtos.AnalyzeResult analyzeReportImage(
      byte[] imageBytes, String userComment, AiDtos.GoalContext goalContext) {
    String base64 = Base64.getEncoder().encodeToString(imageBytes);
    String instruction =
        """
                Analyze this progress report image and user comment.
                Decide whether this report proves meaningful progress for the goal.
                Return strict JSON only with this schema:
                {
                  "verdict": "APPROVED|REJECTED|NEEDS_MORE_INFO",
                  "confidence": 0.0,
                  "explanation": "short and actionable"
                }
                """;
    Map<String, Object> body =
        Map.of(
            "model", appProperties.ai().visionModel(),
            "messages",
                List.of(
                    Map.of(
                        "role",
                        "user",
                        "content",
                        List.of(
                            Map.of(
                                "type",
                                "text",
                                "text",
                                instruction
                                    + "\nGoal title: "
                                    + goalContext.title()
                                    + "\nGoal description: "
                                    + goalContext.description()
                                    + "\nUser comment: "
                                    + userComment),
                            Map.of(
                                "type",
                                "image_url",
                                "image_url",
                                Map.of("url", "data:image/jpeg;base64," + base64))))),
            "response_format", Map.of("type", "json_object"));

    String response =
        webClient
            .post()
            .uri("/chat/completions")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + appProperties.ai().openaiApiKey())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .retrieve()
            .bodyToMono(String.class)
            .block();

    try {
      JsonNode root = objectMapper.readTree(response);
      String content = root.path("choices").get(0).path("message").path("content").asText();
      JsonNode parsed = objectMapper.readTree(content);
      AiVerdict verdict = AiVerdict.valueOf(parsed.path("verdict").asText("NEEDS_MORE_INFO"));
      double confidence = parsed.path("confidence").asDouble(0.5);
      String explanation = parsed.path("explanation").asText("No explanation");
      return new AiDtos.AnalyzeResult(verdict, confidence, explanation);
    } catch (Exception e) {
      return new AiDtos.AnalyzeResult(
          AiVerdict.NEEDS_MORE_INFO, 0.4, "Could not parse AI response.");
    }
  }

  @Override
  public String chat(List<AiDtos.ChatInputMessage> messages, AiDtos.GoalContext goalContext) {
    List<Map<String, String>> msgs =
        messages.stream().map(m -> Map.of("role", m.role(), "content", m.content())).toList();
    String response =
        webClient
            .post()
            .uri("/chat/completions")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + appProperties.ai().openaiApiKey())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
                Map.of(
                    "model", appProperties.ai().textModel(), "messages", msgs, "temperature", 0.4))
            .retrieve()
            .bodyToMono(String.class)
            .block();
    try {
      JsonNode root = objectMapper.readTree(response);
      return root.path("choices").get(0).path("message").path("content").asText();
    } catch (Exception e) {
      return "AI response parsing failed";
    }
  }

  @Override
  public AiDtos.ImageResult generateMotivationImage(
      AiDtos.GoalContext goalContext, String styleOptions) {
    String prompt =
        "Create a motivational image for goal: "
            + goalContext.title()
            + ". Description: "
            + goalContext.description()
            + ". Style: "
            + (styleOptions == null ? "cinematic" : styleOptions);
    String response =
        webClient
            .post()
            .uri("/images/generations")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + appProperties.ai().openaiApiKey())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
                Map.of(
                    "model",
                    appProperties.ai().imageModel(),
                    "prompt",
                    prompt,
                    "size",
                    "1024x1024",
                    "response_format",
                    "b64_json"))
            .retrieve()
            .bodyToMono(String.class)
            .block();

    try {
      JsonNode root = objectMapper.readTree(response);
      JsonNode item = root.path("data").get(0);
      String b64 = item.path("b64_json").asText();
      String path;
      if (b64 != null && !b64.isBlank()) {
        path = storageService.storeMotivationBase64(b64, "png");
      } else {
        String url = item.path("url").asText();
        byte[] bytes = webClient.get().uri(url).retrieve().bodyToMono(byte[].class).block();
        path = storageService.storeBytes(bytes, "motivation", "png");
      }
      return new AiDtos.ImageResult(path, prompt);
    } catch (Exception e) {
      byte[] fallback = "Image generation failed".getBytes(StandardCharsets.UTF_8);
      String path = storageService.storeBytes(fallback, "motivation", "txt");
      return new AiDtos.ImageResult(path, prompt);
    }
  }
}
