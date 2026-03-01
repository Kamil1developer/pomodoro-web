package com.pomodoro.app.dto;

import com.pomodoro.app.enums.AiVerdict;
import com.pomodoro.app.enums.ReportStatus;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public class ReportDtos {
  public record ReportResponse(
      Long id,
      Long goalId,
      LocalDate reportDate,
      String comment,
      String imagePath,
      ReportStatus status,
      AiVerdict aiVerdict,
      String aiExplanation,
      OffsetDateTime createdAt) {}
}
