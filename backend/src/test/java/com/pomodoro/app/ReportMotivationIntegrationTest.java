package com.pomodoro.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pomodoro.app.entity.Goal;
import com.pomodoro.app.entity.MotivationImage;
import com.pomodoro.app.enums.MotivationImageSource;
import com.pomodoro.app.repository.GoalRepository;
import com.pomodoro.app.repository.MotivationImageFeedbackRepository;
import com.pomodoro.app.repository.MotivationImageRepository;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

class ReportMotivationIntegrationTest extends IntegrationTestSupport {

  @Autowired private GoalRepository goalRepository;

  @Autowired private MotivationImageRepository motivationImageRepository;

  @Autowired private MotivationImageFeedbackRepository motivationImageFeedbackRepository;

  @Test
  void refreshFeedShouldCreateFallbackCardsWhenExternalSourceIsEmptyOrUnavailable()
      throws Exception {
    Tokens tokens = registerUser("mot-refresh@test.dev", "password123");
    Long goalId = createGoal(tokens.accessToken(), "Выучить английский");

    mockMvc
        .perform(
            post("/api/goals/{goalId}/motivation/refresh-feed", goalId)
                .header("Authorization", bearer(tokens.accessToken())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.images.length()").value(org.hamcrest.Matchers.greaterThan(0)));

    mockMvc
        .perform(
            get("/api/motivation/feed")
                .param("goalId", goalId.toString())
                .param("limit", "10")
                .header("Authorization", bearer(tokens.accessToken())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.images.length()").value(org.hamcrest.Matchers.greaterThan(0)));
  }

  @Test
  void motivationFeedShouldReturnUpToTenImagesIfAvailable() throws Exception {
    Tokens tokens = registerUser("mot-feed@test.dev", "password123");
    Long goalId = createGoal(tokens.accessToken(), "Sport goal");
    Goal goal = goalRepository.findById(goalId).orElseThrow();
    for (int i = 0; i < 12; i++) {
      saveMotivationImage(goal, "SPORT", "https://images.example/sport-" + i + ".jpg");
    }

    mockMvc
        .perform(
            get("/api/motivation/feed")
                .param("goalId", goalId.toString())
                .param("limit", "10")
                .header("Authorization", bearer(tokens.accessToken())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.images.length()").value(10))
        .andExpect(jsonPath("$.quote.quoteText").isString())
        .andExpect(jsonPath("$.recommendation").isString());
  }

  @Test
  void imageMarkedNotInterestedShouldNotAppearAgainForSameUser() throws Exception {
    Tokens owner = registerUser("mot-hide-owner@test.dev", "password123");
    Long goalId = createGoal(owner.accessToken(), "Sport goal");
    Goal goal = goalRepository.findById(goalId).orElseThrow();
    MotivationImage image = saveMotivationImage(goal, "SPORT", "https://images.example/same-1.jpg");

    mockMvc
        .perform(
            post("/api/motivation/cards/{imageId}/not-interested", image.getId())
                .header("Authorization", bearer(owner.accessToken())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("OK"));

    mockMvc
        .perform(
            get("/api/motivation/feed")
                .param("goalId", goalId.toString())
                .param("limit", "10")
                .header("Authorization", bearer(owner.accessToken())))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath(
                "$.images[*].id", not(org.hamcrest.Matchers.hasItem(image.getId().intValue()))));
  }

  @Test
  void imageMarkedNotInterestedCanStillAppearForAnotherUser() throws Exception {
    Tokens owner = registerUser("mot-hide-owner-2@test.dev", "password123");
    Tokens other = registerUser("mot-hide-other@test.dev", "password123");
    Long ownerGoalId = createGoal(owner.accessToken(), "Sport goal");
    Long otherGoalId = createGoal(other.accessToken(), "Sport streak");
    Goal ownerGoal = goalRepository.findById(ownerGoalId).orElseThrow();
    MotivationImage image =
        saveMotivationImage(ownerGoal, "SPORT", "https://images.example/shared.jpg");

    mockMvc
        .perform(
            post("/api/motivation/images/{imageId}/not-interested", image.getId())
                .header("Authorization", bearer(owner.accessToken())))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            get("/api/motivation/feed")
                .param("goalId", otherGoalId.toString())
                .param("limit", "10")
                .header("Authorization", bearer(other.accessToken())))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.images[*].id")
                .value(org.hamcrest.Matchers.hasItem(image.getId().intValue())));
  }

  @Test
  void reportedImageShouldNotAppearAgainForSameUser() throws Exception {
    Tokens owner = registerUser("mot-report-owner@test.dev", "password123");
    Long goalId = createGoal(owner.accessToken(), "Sport goal");
    Goal goal = goalRepository.findById(goalId).orElseThrow();
    MotivationImage image =
        saveMotivationImage(goal, "SPORT", "https://images.example/report-1.jpg");

    mockMvc
        .perform(
            post("/api/motivation/cards/{imageId}/report", image.getId())
                .header("Authorization", bearer(owner.accessToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "reason": "LOW_QUALITY",
                      "comment": "слишком размыто"
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.message")
                .value("Жалоба отправлена. Мы больше не будем показывать эту карточку."));

    mockMvc
        .perform(
            get("/api/motivation/feed")
                .param("goalId", goalId.toString())
                .param("limit", "10")
                .header("Authorization", bearer(owner.accessToken())))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath(
                "$.images[*].id", not(org.hamcrest.Matchers.hasItem(image.getId().intValue()))));
  }

  @Test
  void imageWithThreeReportsShouldBeHiddenGlobally() throws Exception {
    Tokens first = registerUser("mot-global-1@test.dev", "password123");
    Tokens second = registerUser("mot-global-2@test.dev", "password123");
    Tokens third = registerUser("mot-global-3@test.dev", "password123");
    Tokens fourth = registerUser("mot-global-4@test.dev", "password123");
    Long goalId1 = createGoal(first.accessToken(), "Sport goal");
    Long goalId2 = createGoal(second.accessToken(), "Sport plan");
    Long goalId3 = createGoal(third.accessToken(), "Sport discipline");
    Long goalId4 = createGoal(fourth.accessToken(), "Sport focus");
    Goal goal = goalRepository.findById(goalId1).orElseThrow();
    MotivationImage image =
        saveMotivationImage(goal, "SPORT", "https://images.example/global-hide.jpg");

    reportImage(first.accessToken(), image.getId());
    reportImage(second.accessToken(), image.getId());
    reportImage(third.accessToken(), image.getId());

    MotivationImage stored = motivationImageRepository.findById(image.getId()).orElseThrow();
    assertThat(stored.getHiddenGlobally()).isTrue();
    assertThat(stored.getReportCount()).isEqualTo(3);

    mockMvc
        .perform(
            get("/api/motivation/feed")
                .param("goalId", goalId4.toString())
                .param("limit", "10")
                .header("Authorization", bearer(fourth.accessToken())))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath(
                "$.images[*].id", not(org.hamcrest.Matchers.hasItem(image.getId().intValue()))));

    assertThat(goalId2).isNotNull();
    assertThat(goalId3).isNotNull();
  }

  @Test
  void duplicateFeedbackShouldNotCreateDuplicateRecords() throws Exception {
    Tokens owner = registerUser("mot-dup@test.dev", "password123");
    Long goalId = createGoal(owner.accessToken(), "Sport goal");
    Goal goal = goalRepository.findById(goalId).orElseThrow();
    Long userId = goal.getUser().getId();
    MotivationImage image = saveMotivationImage(goal, "SPORT", "https://images.example/dup.jpg");

    mockMvc
        .perform(
            post("/api/motivation/images/{imageId}/not-interested", image.getId())
                .header("Authorization", bearer(owner.accessToken())))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            post("/api/motivation/images/{imageId}/not-interested", image.getId())
                .header("Authorization", bearer(owner.accessToken())))
        .andExpect(status().isOk());

    assertThat(
            motivationImageFeedbackRepository.findByUserId(userId).stream()
                .filter(feedback -> feedback.getImage().getId().equals(image.getId()))
                .count())
        .isEqualTo(1);
  }

  @Test
  void unauthenticatedMotivationFeedbackRequestShouldBeRejected() throws Exception {
    Tokens owner = registerUser("mot-unauth@test.dev", "password123");
    Long goalId = createGoal(owner.accessToken(), "Sport goal");
    Goal goal = goalRepository.findById(goalId).orElseThrow();
    MotivationImage image =
        saveMotivationImage(goal, "SPORT", "https://images.example/private.jpg");

    mockMvc
        .perform(post("/api/motivation/images/{imageId}/not-interested", image.getId()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void aiReportVerificationShouldNotApproveEmptyProof() throws Exception {
    Tokens tokens = registerUser("report-empty@test.dev", "password123");
    Long goalId = createGoal(tokens.accessToken(), "Java goal");
    createTask(tokens.accessToken(), goalId, "Написать код сервиса");

    MockMultipartFile file =
        new MockMultipartFile("file", "proof.jpg", "image/jpeg", "fake-image-data".getBytes());
    MockMultipartFile comment = new MockMultipartFile("comment", "", "text/plain", "".getBytes());

    mockMvc
        .perform(
            multipart("/api/goals/{goalId}/reports", goalId)
                .file(file)
                .file(comment)
                .header("Authorization", bearer(tokens.accessToken())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.aiVerdict").value("NEEDS_MORE_INFO"))
        .andExpect(jsonPath("$.status").value("PENDING"));
  }

  @Test
  void mockAiShouldNotApproveWatchedVideoForCodeTask() throws Exception {
    Tokens tokens = registerUser("report-video@test.dev", "password123");
    Long goalId = createGoal(tokens.accessToken(), "Java goal");
    createTask(tokens.accessToken(), goalId, "Написать код сервиса и запустить тесты");

    MockMultipartFile file =
        new MockMultipartFile("file", "proof.jpg", "image/jpeg", "fake-image-data".getBytes());
    MockMultipartFile comment =
        new MockMultipartFile(
            "comment", "", "text/plain", "смотрел видео на YouTube по Java, готово".getBytes());

    mockMvc
        .perform(
            multipart("/api/goals/{goalId}/reports", goalId)
                .file(file)
                .file(comment)
                .header("Authorization", bearer(tokens.accessToken())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.aiVerdict").value("NEEDS_MORE_INFO"))
        .andExpect(jsonPath("$.status").value("PENDING"));
  }

  @Test
  void mockAiCanApproveVideoOnlyForLectureTask() throws Exception {
    Tokens tokens = registerUser("report-lecture@test.dev", "password123");
    Long goalId = createGoal(tokens.accessToken(), "Study goal");
    createTask(tokens.accessToken(), goalId, "Посмотреть лекцию по алгоритмам");

    MockMultipartFile file =
        new MockMultipartFile("file", "proof.jpg", "image/jpeg", "fake-image-data".getBytes());
    MockMultipartFile comment =
        new MockMultipartFile(
            "comment", "", "text/plain", "посмотрел видео лекции по алгоритмам, готово".getBytes());

    mockMvc
        .perform(
            multipart("/api/goals/{goalId}/reports", goalId)
                .file(file)
                .file(comment)
                .header("Authorization", bearer(tokens.accessToken())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.aiVerdict").value("APPROVED"))
        .andExpect(jsonPath("$.status").value("CONFIRMED"));
  }

  @Test
  void uploadReportShouldReturnAiConfidenceAndStatus() throws Exception {
    Tokens tokens = registerUser("report1@test.dev", "password123");
    Long goalId = createGoal(tokens.accessToken(), "Goal report");
    createTask(tokens.accessToken(), goalId, "Сделать тренировку");

    MockMultipartFile file =
        new MockMultipartFile("file", "proof.jpg", "image/jpeg", "fake-image-data".getBytes());
    MockMultipartFile comment =
        new MockMultipartFile("comment", "", "text/plain", "сделал тренировку, готово".getBytes());

    mockMvc
        .perform(
            multipart("/api/goals/{goalId}/reports", goalId)
                .file(file)
                .file(comment)
                .header("Authorization", bearer(tokens.accessToken())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.aiVerdict").value("APPROVED"))
        .andExpect(jsonPath("$.status").value("CONFIRMED"))
        .andExpect(jsonPath("$.aiConfidence").isNumber())
        .andExpect(
            jsonPath("$.imagePath").value(org.hamcrest.Matchers.startsWith("/uploads/reports/")));
  }

  @Test
  void generateMotivationShouldSupportFavoriteAndDelete() throws Exception {
    Tokens tokens = registerUser("report2@test.dev", "password123");
    Long goalId = createGoal(tokens.accessToken(), "Goal motivation");

    String response =
        mockMvc
            .perform(
                post("/api/goals/{goalId}/motivation/generate", goalId)
                    .header("Authorization", bearer(tokens.accessToken()))
                    .contentType("application/json")
                    .content(
                        """
                                {
                                  "styleOptions": "minimal poster"
                                }
                                """))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.imagePath")
                    .value(org.hamcrest.Matchers.startsWith("/uploads/motivation/")))
            .andExpect(jsonPath("$.isFavorite").value(false))
            .andExpect(jsonPath("$.generatedBy").value("MANUAL"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    Long imageId = objectMapper.readTree(response).get("id").asLong();

    mockMvc
        .perform(
            patch("/api/motivation/{id}/favorite", imageId)
                .header("Authorization", bearer(tokens.accessToken()))
                .contentType("application/json")
                .content(
                    """
                                {
                                  "isFavorite": true
                                }
                                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.isFavorite").value(true))
        .andExpect(jsonPath("$.isPinned").value(true))
        .andExpect(jsonPath("$.pinnedUntil").isString());

    mockMvc
        .perform(
            delete("/api/motivation/{id}", imageId)
                .header("Authorization", bearer(tokens.accessToken())))
        .andExpect(status().isOk());
  }

  private void reportImage(String accessToken, Long imageId) throws Exception {
    mockMvc
        .perform(
            post("/api/motivation/images/{imageId}/report", imageId)
                .header("Authorization", bearer(accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "reason": "IRRELEVANT"
                    }
                    """))
        .andExpect(status().isOk());
  }

  private MotivationImage saveMotivationImage(Goal goal, String theme, String imageUrl) {
    return motivationImageRepository.save(
        MotivationImage.builder()
            .goal(goal)
            .imagePath(imageUrl)
            .sourceUrl(imageUrl)
            .title("Image " + imageUrl.substring(imageUrl.lastIndexOf('/') + 1))
            .description("Подобрано по теме")
            .theme(theme)
            .prompt("feed")
            .isFavorite(false)
            .generatedBy(MotivationImageSource.AUTO)
            .hiddenGlobally(false)
            .reportCount(0)
            .createdAt(OffsetDateTime.now())
            .build());
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
}
