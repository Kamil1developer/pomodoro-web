package com.pomodoro.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pomodoro.app.entity.FocusSession;
import com.pomodoro.app.entity.GoalCommitment;
import com.pomodoro.app.entity.Report;
import com.pomodoro.app.enums.CommitmentStatus;
import com.pomodoro.app.enums.RiskStatus;
import com.pomodoro.app.repository.FocusSessionRepository;
import com.pomodoro.app.repository.GoalCommitmentRepository;
import com.pomodoro.app.repository.GoalRepository;
import com.pomodoro.app.repository.ReportRepository;
import com.pomodoro.app.service.GoalCommitmentService;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

class GoalExperienceIntegrationTest extends IntegrationTestSupport {

  @Autowired private GoalCommitmentRepository goalCommitmentRepository;

  @Autowired private GoalCommitmentService goalCommitmentService;

  @Autowired private FocusSessionRepository focusSessionRepository;

  @Autowired private GoalRepository goalRepository;

  @Autowired private ReportRepository reportRepository;

  @Test
  void createCommitmentForGoalShouldWork() throws Exception {
    Tokens tokens = registerUser("commitment1@test.dev", "password123");
    Long goalId = createGoal(tokens.accessToken(), "Goal commitment");

    mockMvc
        .perform(
            post("/api/goals/{goalId}/commitment", goalId)
                .header("Authorization", bearer(tokens.accessToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "dailyTargetMinutes": 90,
                      "startDate": "%s",
                      "endDate": "%s",
                      "personalRewardTitle": "Новый велосипед",
                      "personalRewardDescription": "Покупка после завершения обязательства"
                    }
                    """
                        .formatted(LocalDate.now(), LocalDate.now().plusDays(21))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.goalId").value(goalId))
        .andExpect(jsonPath("$.dailyTargetMinutes").value(90))
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.disciplineScore").value(80))
        .andExpect(jsonPath("$.riskStatus").value("LOW"));
  }

  @Test
  void shouldRejectSecondActiveCommitmentForSameGoal() throws Exception {
    Tokens tokens = registerUser("commitment2@test.dev", "password123");
    Long goalId = createGoal(tokens.accessToken(), "Goal duplicate commitment");

    createCommitment(
        tokens.accessToken(), goalId, 45, LocalDate.now(), LocalDate.now().plusDays(14));

    mockMvc
        .perform(
            post("/api/goals/{goalId}/commitment", goalId)
                .header("Authorization", bearer(tokens.accessToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "dailyTargetMinutes": 60,
                      "startDate": "%s"
                    }
                    """
                        .formatted(LocalDate.now())))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.message")
                .value(org.hamcrest.Matchers.containsString("активное ежедневное обязательство")));
  }

  @Test
  void getTodayStatusShouldReflectFocusAndConfirmedReport() throws Exception {
    Tokens tokens = registerUser("commitment3@test.dev", "password123");
    Long goalId = createGoal(tokens.accessToken(), "Goal today status");
    createCommitment(
        tokens.accessToken(), goalId, 1, LocalDate.now(), LocalDate.now().plusDays(14));
    createTask(tokens.accessToken(), goalId, "Сделать тренировку");

    mockMvc
        .perform(
            post("/api/goals/{goalId}/focus/start", goalId)
                .header("Authorization", bearer(tokens.accessToken())))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/api/goals/{goalId}/focus/stop", goalId)
                .header("Authorization", bearer(tokens.accessToken())))
        .andExpect(status().isOk());

    uploadApprovedReport(tokens.accessToken(), goalId, "сделал тренировку, готово");

    mockMvc
        .perform(
            get("/api/goals/{goalId}/today", goalId)
                .header("Authorization", bearer(tokens.accessToken())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.dailyTargetMinutes").value(1))
        .andExpect(
            jsonPath("$.completedFocusMinutesToday")
                .value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
        .andExpect(jsonPath("$.reportStatusToday").value("CONFIRMED"))
        .andExpect(jsonPath("$.hasApprovedReportToday").value(true))
        .andExpect(jsonPath("$.isDailyTargetReached").value(true))
        .andExpect(jsonPath("$.isTodayCompleted").value(true));
  }

  @Test
  void reportShouldStoreAiConfidence() throws Exception {
    Tokens tokens = registerUser("commitment4@test.dev", "password123");
    Long goalId = createGoal(tokens.accessToken(), "Goal ai confidence");
    createTask(tokens.accessToken(), goalId, "Сделать тренировку");

    String response =
        uploadApprovedReport(tokens.accessToken(), goalId, "сделал тренировку, готово");
    Long reportId = objectMapper.readTree(response).get("id").asLong();

    mockMvc
        .perform(
            get("/api/goals/{goalId}/reports", goalId)
                .header("Authorization", bearer(tokens.accessToken())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].aiConfidence").isNumber());

    Report report = reportRepository.findById(reportId).orElseThrow();
    assertThat(report.getAiConfidence()).isNotNull();
    assertThat(report.getAiConfidence()).isGreaterThan(0.5d);
  }

  @Test
  void focusStopShouldCreateGoalEvent() throws Exception {
    Tokens tokens = registerUser("commitment5@test.dev", "password123");
    Long goalId = createGoal(tokens.accessToken(), "Goal focus event");
    createCommitment(
        tokens.accessToken(), goalId, 20, LocalDate.now(), LocalDate.now().plusDays(7));
    createTask(tokens.accessToken(), goalId, "Сделать задачу дня");

    mockMvc
        .perform(
            post("/api/goals/{goalId}/focus/start", goalId)
                .header("Authorization", bearer(tokens.accessToken())))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/api/goals/{goalId}/focus/stop", goalId)
                .header("Authorization", bearer(tokens.accessToken())))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            get("/api/goals/{goalId}/events", goalId)
                .header("Authorization", bearer(tokens.accessToken())))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$[*].type", hasItems("FOCUS_SESSION_STARTED", "FOCUS_SESSION_COMPLETED")));
  }

  @Test
  void reportUploadShouldCreateReportEvents() throws Exception {
    Tokens tokens = registerUser("commitment6@test.dev", "password123");
    Long goalId = createGoal(tokens.accessToken(), "Goal report event");
    createCommitment(
        tokens.accessToken(), goalId, 20, LocalDate.now(), LocalDate.now().plusDays(7));
    createTask(tokens.accessToken(), goalId, "Сделать тренировку");

    uploadApprovedReport(tokens.accessToken(), goalId, "сделал тренировку, готово");

    mockMvc
        .perform(
            get("/api/goals/{goalId}/events", goalId)
                .header("Authorization", bearer(tokens.accessToken())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].type", hasItems("REPORT_SUBMITTED", "REPORT_APPROVED")));
  }

  @Test
  void processPreviousDayShouldMarkCompletedDayAndIncreaseDiscipline() throws Exception {
    Tokens tokens = registerUser("commitment7@test.dev", "password123");
    Long goalId = createGoal(tokens.accessToken(), "Goal completed day");
    createCommitment(
        tokens.accessToken(),
        goalId,
        1,
        LocalDate.now().minusDays(2),
        LocalDate.now().plusDays(10));
    createTask(tokens.accessToken(), goalId, "Сделать тренировку");

    mockMvc
        .perform(
            post("/api/goals/{goalId}/focus/start", goalId)
                .header("Authorization", bearer(tokens.accessToken())))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/api/goals/{goalId}/focus/stop", goalId)
                .header("Authorization", bearer(tokens.accessToken())))
        .andExpect(status().isOk());

    String reportResponse =
        uploadApprovedReport(tokens.accessToken(), goalId, "сделал тренировку, готово");
    Long reportId = objectMapper.readTree(reportResponse).get("id").asLong();
    LocalDate targetDate = LocalDate.now().minusDays(1);
    moveLatestSessionToDate(goalId, targetDate);
    Report report = reportRepository.findById(reportId).orElseThrow();
    report.setReportDate(targetDate);
    reportRepository.save(report);

    goalCommitmentService.processPreviousDay(targetDate);

    Long ownerId = goalRepository.findById(goalId).orElseThrow().getUser().getId();
    GoalCommitment commitment =
        goalCommitmentRepository
            .findByGoalIdAndUserIdAndStatus(goalId, ownerId, CommitmentStatus.ACTIVE)
            .orElseThrow();
    assertThat(commitment.getCompletedDays()).isEqualTo(1);
    assertThat(commitment.getCurrentStreak()).isEqualTo(1);
    assertThat(commitment.getBestStreak()).isEqualTo(1);
    assertThat(commitment.getDisciplineScore()).isEqualTo(83);
    assertThat(commitment.getRiskStatus()).isEqualTo(RiskStatus.LOW);
  }

  @Test
  void processPreviousDayShouldMarkMissedDayAndEscalateRisk() throws Exception {
    Tokens tokens = registerUser("commitment8@test.dev", "password123");
    Long goalId = createGoal(tokens.accessToken(), "Goal missed day");
    createCommitment(
        tokens.accessToken(),
        goalId,
        30,
        LocalDate.now().minusDays(5),
        LocalDate.now().plusDays(30));

    LocalDate firstDate = LocalDate.now().minusDays(4);
    goalCommitmentService.processPreviousDay(firstDate);
    goalCommitmentService.processPreviousDay(firstDate.plusDays(1));
    goalCommitmentService.processPreviousDay(firstDate.plusDays(2));
    goalCommitmentService.processPreviousDay(firstDate.plusDays(3));

    Long ownerId = goalRepository.findById(goalId).orElseThrow().getUser().getId();
    GoalCommitment commitment =
        goalCommitmentRepository
            .findByGoalIdAndUserIdAndStatus(goalId, ownerId, CommitmentStatus.ACTIVE)
            .orElseThrow();
    assertThat(commitment.getMissedDays()).isEqualTo(4);
    assertThat(commitment.getCurrentStreak()).isZero();
    assertThat(commitment.getDisciplineScore()).isEqualTo(40);
    assertThat(commitment.getRiskStatus()).isEqualTo(RiskStatus.HIGH);

    mockMvc
        .perform(
            get("/api/goals/{goalId}/events", goalId)
                .header("Authorization", bearer(tokens.accessToken())))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath(
                "$[*].type",
                hasItems("DAY_MISSED", "DISCIPLINE_SCORE_CHANGED", "RISK_STATUS_CHANGED")));
  }

  @Test
  void otherUserShouldNotReadForeignGoalExperience() throws Exception {
    Tokens owner = registerUser("commitment9-owner@test.dev", "password123");
    Tokens stranger = registerUser("commitment9-stranger@test.dev", "password123");
    Long goalId = createGoal(owner.accessToken(), "Foreign goal");
    createCommitment(
        owner.accessToken(), goalId, 45, LocalDate.now(), LocalDate.now().plusDays(10));

    mockMvc
        .perform(
            get("/api/goals/{goalId}/experience", goalId)
                .header("Authorization", bearer(stranger.accessToken())))
        .andExpect(status().isNotFound());
  }

  private void createTask(String accessToken, Long goalId, String title) throws Exception {
    mockMvc
        .perform(
            post("/api/goals/{goalId}/tasks", goalId)
                .header("Authorization", bearer(accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "title": "%s"
                    }
                    """
                        .formatted(title)))
        .andExpect(status().isOk());
  }

  private void createCommitment(
      String accessToken,
      Long goalId,
      int dailyTargetMinutes,
      LocalDate startDate,
      LocalDate endDate)
      throws Exception {
    mockMvc
        .perform(
            post("/api/goals/{goalId}/commitment", goalId)
                .header("Authorization", bearer(accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "dailyTargetMinutes": %d,
                      "startDate": "%s",
                      "endDate": "%s",
                      "personalRewardTitle": "Награда",
                      "personalRewardDescription": "Описание награды"
                    }
                    """
                        .formatted(dailyTargetMinutes, startDate, endDate)))
        .andExpect(status().isOk());
  }

  private String uploadApprovedReport(String accessToken, Long goalId, String commentText)
      throws Exception {
    MockMultipartFile file =
        new MockMultipartFile("file", "proof.jpg", "image/jpeg", "fake-image-data".getBytes());
    MockMultipartFile comment =
        new MockMultipartFile("comment", "", "text/plain", commentText.getBytes());

    return mockMvc
        .perform(
            multipart("/api/goals/{goalId}/reports", goalId)
                .file(file)
                .file(comment)
                .header("Authorization", bearer(accessToken)))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
  }

  private void moveLatestSessionToDate(Long goalId, LocalDate targetDate) {
    FocusSession session =
        focusSessionRepository.findByGoalIdOrderByStartedAtDesc(goalId).getFirst();
    session.setStartedAt(
        targetDate.atTime(10, 0).atZone(ZoneId.systemDefault()).toOffsetDateTime());
    session.setEndedAt(targetDate.atTime(10, 1).atZone(ZoneId.systemDefault()).toOffsetDateTime());
    session.setDurationMinutes(1);
    focusSessionRepository.save(session);
  }
}
