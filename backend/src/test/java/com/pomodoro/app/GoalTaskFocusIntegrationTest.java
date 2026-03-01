package com.pomodoro.app;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class GoalTaskFocusIntegrationTest extends IntegrationTestSupport {

  @Test
  void securedEndpointShouldReturnUnauthorizedWithoutToken() throws Exception {
    mockMvc
        .perform(get("/api/goals"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
  }

  @Test
  void shouldCreateTaskAndReflectProgress() throws Exception {
    Tokens tokens = registerUser("goal1@test.dev", "password123");
    Long goalId = createGoal(tokens.accessToken(), "Goal progress");

    String taskResponse =
        mockMvc
            .perform(
                post("/api/goals/{goalId}/tasks", goalId)
                    .header("Authorization", bearer(tokens.accessToken()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                                {
                                  "title": "Finish API"
                                }
                                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isDone").value(false))
            .andReturn()
            .getResponse()
            .getContentAsString();

    Long taskId = objectMapper.readTree(taskResponse).get("id").asLong();

    mockMvc
        .perform(
            put("/api/goals/{goalId}/tasks/{taskId}", goalId, taskId)
                .header("Authorization", bearer(tokens.accessToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "title": "Finish API",
                                  "isDone": true
                                }
                                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.isDone").value(true));

    mockMvc
        .perform(
            get("/api/goals/{goalId}/progress", goalId)
                .header("Authorization", bearer(tokens.accessToken())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.completedTasks").value(1))
        .andExpect(jsonPath("$.allTasks").value(1));
  }

  @Test
  void shouldStartAndStopFocusSession() throws Exception {
    Tokens tokens = registerUser("goal2@test.dev", "password123");
    Long goalId = createGoal(tokens.accessToken(), "Goal focus");

    mockMvc
        .perform(
            post("/api/goals/{goalId}/focus/start", goalId)
                .header("Authorization", bearer(tokens.accessToken())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.startedAt").isString())
        .andExpect(jsonPath("$.endedAt").value(nullValue()));

    mockMvc
        .perform(
            post("/api/goals/{goalId}/focus/stop", goalId)
                .header("Authorization", bearer(tokens.accessToken())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.durationMinutes").isNumber())
        .andExpect(jsonPath("$.endedAt").isString());
  }
}
