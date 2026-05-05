package com.pomodoro.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.pomodoro.app.entity.Goal;
import com.pomodoro.app.enums.GoalStatus;
import com.pomodoro.app.repository.GoalRepository;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

class GoalProfileIntegrationTest extends IntegrationTestSupport {

  @Autowired private GoalRepository goalRepository;

  @Test
  void duplicateActiveGoalShouldBeRejected() throws Exception {
    Tokens tokens = registerUser("goal-duplicate@test.dev", "password123");
    createGoal(tokens.accessToken(), "Выучить английский");

    mockMvc
        .perform(
            post("/api/goals")
                .header("Authorization", bearer(tokens.accessToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "title": "выучить   английский!",
                      "description": "ещё одна похожая цель",
                      "targetHours": 20
                    }
                    """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message", containsString("Похожая активная цель уже существует")));
  }

  @Test
  void similarActiveGoalShouldBeRejected() throws Exception {
    Tokens tokens = registerUser("goal-similar@test.dev", "password123");
    createGoal(tokens.accessToken(), "Выучить английский");

    mockMvc
        .perform(
            post("/api/goals")
                .header("Authorization", bearer(tokens.accessToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "title": "Начать изучать английский язык",
                      "description": "похожая активная цель",
                      "targetHours": 25
                    }
                    """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message", containsString("Выучить английский")));
  }

  @Test
  void failedOrCompletedGoalAllowsCreatingSimilarNewGoal() throws Exception {
    Tokens failedTokens = registerUser("goal-retry-failed@test.dev", "password123");
    Long failedGoalId = createGoal(failedTokens.accessToken(), "Выучить английский");

    mockMvc
        .perform(
            post("/api/goals/{id}/close-failed", failedGoalId)
                .header("Authorization", bearer(failedTokens.accessToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "reason": "Не хватило времени"
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("FAILED"));

    mockMvc
        .perform(
            post("/api/goals")
                .header("Authorization", bearer(failedTokens.accessToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "title": "Начать изучать английский язык",
                      "description": "новая попытка",
                      "targetHours": 15
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ACTIVE"));

    Tokens completedTokens = registerUser("goal-retry-completed@test.dev", "password123");
    Long completedGoalId = createGoal(completedTokens.accessToken(), "Учить Java");
    Goal completedGoal = goalRepository.findById(completedGoalId).orElseThrow();
    completedGoal.setStatus(GoalStatus.COMPLETED);
    completedGoal.setCompletedAt(OffsetDateTime.now());
    completedGoal.setClosedAt(OffsetDateTime.now());
    goalRepository.save(completedGoal);

    mockMvc
        .perform(
            post("/api/goals")
                .header("Authorization", bearer(completedTokens.accessToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "title": "Начать изучать Java глубже",
                      "description": "следующий этап",
                      "targetHours": 30
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ACTIVE"));
  }

  @Test
  void profileShouldReturnStatsAvatarAndHistory() throws Exception {
    Tokens tokens = registerUser("profile@test.dev", "password123");
    Long activeGoalId = createGoal(tokens.accessToken(), "Сделать pet-project");
    Long failedGoalId = createGoal(tokens.accessToken(), "Сдать сложную цель");

    mockMvc
        .perform(
            post("/api/goals/{id}/close-failed", failedGoalId)
                .header("Authorization", bearer(tokens.accessToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "reason": "Потерял интерес"
                    }
                    """))
        .andExpect(status().isOk());

    MockMultipartFile avatar =
        new MockMultipartFile("file", "avatar.png", "image/png", "avatar-bytes".getBytes());

    mockMvc
        .perform(
            multipart("/api/profile/avatar")
                .file(avatar)
                .header("Authorization", bearer(tokens.accessToken())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.avatarPath", containsString("/uploads/avatars/")));

    mockMvc
        .perform(get("/api/profile").header("Authorization", bearer(tokens.accessToken())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("profile@test.dev"))
        .andExpect(jsonPath("$.stats.activeGoalsCount").value(1))
        .andExpect(jsonPath("$.stats.failedGoalsCount").value(1))
        .andExpect(jsonPath("$.activeGoals[0].goalId").value(activeGoalId))
        .andExpect(jsonPath("$.goalHistory[0].goalId").value(failedGoalId))
        .andExpect(jsonPath("$.goalHistory[0].failureReason").value("Потерял интерес"))
        .andExpect(jsonPath("$.goalHistory[0].loserBadge").value(true));
  }

  @Test
  void closingGoalAsFailedRequiresReason() throws Exception {
    Tokens tokens = registerUser("goal-close-reason@test.dev", "password123");
    Long goalId = createGoal(tokens.accessToken(), "Закрыть цель");

    mockMvc
        .perform(
            post("/api/goals/{id}/close-failed", goalId)
                .header("Authorization", bearer(tokens.accessToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "reason": ""
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void motivatorShouldUseGoalProgressContextInFallback() throws Exception {
    Tokens tokens = registerUser("motivator-context@test.dev", "password123");
    Long goalId = createGoal(tokens.accessToken(), "Подготовить диплом");
    createTask(tokens.accessToken(), goalId, "Закончить практическую часть");
    createCommitment(tokens.accessToken(), goalId, 50);

    String response =
        mockMvc
            .perform(
                post("/api/goals/{goalId}/chat/send", goalId)
                    .header("Authorization", bearer(tokens.accessToken()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "content": "Что мне сделать сегодня?"
                        }
                        """))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    JsonNode node = objectMapper.readTree(response);
    JsonNode lastMessage = node.get("messages").get(node.get("messages").size() - 1);
    String content =
        new String(
            lastMessage.get("content").asText().getBytes(StandardCharsets.ISO_8859_1),
            StandardCharsets.UTF_8);

    assertThat(content).contains("дневной нормы");
    assertThat(content).contains("фото-отчёт");
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

  private void createCommitment(String accessToken, Long goalId, int dailyTargetMinutes)
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
                      "startDate": "2026-05-01",
                      "endDate": "2026-05-31",
                      "personalRewardTitle": "Награда",
                      "personalRewardDescription": "Отдых"
                    }
                    """
                        .formatted(dailyTargetMinutes)))
        .andExpect(status().isOk());
  }
}
