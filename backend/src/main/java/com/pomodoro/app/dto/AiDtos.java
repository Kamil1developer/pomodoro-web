package com.pomodoro.app.dto;

import com.pomodoro.app.enums.AiVerdict;
import java.util.List;

public class AiDtos {
  public record AnalyzeResult(AiVerdict verdict, Double confidence, String explanation) {}

  public record ChatInputMessage(String role, String content) {}

  public record GoalContext(Long goalId, String title, String description) {}

  public record ImageResult(String imagePath, String usedPrompt) {}

  public record OpenAiTextRequest(String model, List<Message> messages, Double temperature) {
    public record Message(String role, String content) {}
  }

  public record OpenAiImageRequest(String model, String prompt, String size) {}
}
