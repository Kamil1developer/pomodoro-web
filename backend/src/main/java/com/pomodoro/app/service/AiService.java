package com.pomodoro.app.service;

import com.pomodoro.app.dto.AiDtos;
import java.util.List;

public interface AiService {
  AiDtos.AnalyzeResult analyzeReportImage(
      byte[] imageBytes, String userComment, AiDtos.GoalContext goalContext);

  String chat(List<AiDtos.ChatInputMessage> messages, AiDtos.GoalContext goalContext);

  AiDtos.ImageResult generateMotivationImage(AiDtos.GoalContext goalContext, String styleOptions);
}
