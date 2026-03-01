package com.pomodoro.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pomodoro.app.entity.Report;
import com.pomodoro.app.enums.ReportStatus;
import com.pomodoro.app.repository.DailySummaryRepository;
import com.pomodoro.app.repository.ReportRepository;
import com.pomodoro.app.scheduler.DailyScheduler;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

class ChatSchedulerIntegrationTest extends IntegrationTestSupport {

  @Autowired private DailyScheduler dailyScheduler;

  @Autowired private ReportRepository reportRepository;

  @Autowired private DailySummaryRepository dailySummaryRepository;

  @Test
  void chatShouldPersistConversationHistory() throws Exception {
    Tokens tokens = registerUser("chat1@test.dev", "password123");
    Long goalId = createGoal(tokens.accessToken(), "Goal chat");

    mockMvc
        .perform(
            post("/api/goals/{goalId}/chat/send", goalId)
                .header("Authorization", bearer(tokens.accessToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "content": "Что делать следующим шагом?"
                                }
                                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.messages.length()").value(2))
        .andExpect(jsonPath("$.messages[0].role").value("USER"))
        .andExpect(jsonPath("$.messages[1].role").value("ASSISTANT"));

    mockMvc
        .perform(
            get("/api/goals/{goalId}/chat/history", goalId)
                .header("Authorization", bearer(tokens.accessToken())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.messages.length()").value(2));
  }

  @Test
  void schedulerShouldMarkOldPendingReportsAsOverdueAndCreateSummary() throws Exception {
    Tokens tokens = registerUser("chat2@test.dev", "password123");
    Long goalId = createGoal(tokens.accessToken(), "Goal scheduler");

    MockMultipartFile file =
        new MockMultipartFile("file", "proof.jpg", "image/jpeg", "fake-image-data".getBytes());
    MockMultipartFile comment =
        new MockMultipartFile("comment", "", "text/plain", "later".getBytes());

    String reportResponse =
        mockMvc
            .perform(
                multipart("/api/goals/{goalId}/reports", goalId)
                    .file(file)
                    .file(comment)
                    .header("Authorization", bearer(tokens.accessToken())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    Long reportId = objectMapper.readTree(reportResponse).get("id").asLong();
    Report report = reportRepository.findById(reportId).orElseThrow();
    report.setReportDate(LocalDate.now().minusDays(1));
    reportRepository.save(report);

    dailyScheduler.closeDay();

    Report updated = reportRepository.findById(reportId).orElseThrow();
    assertThat(updated.getStatus()).isEqualTo(ReportStatus.OVERDUE);
    assertThat(
            dailySummaryRepository.findByGoalIdAndSummaryDate(goalId, LocalDate.now().minusDays(1)))
        .isPresent();
  }
}
