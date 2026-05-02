package com.pomodoro.app;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

class ReportMotivationIntegrationTest extends IntegrationTestSupport {

  @Test
  void uploadReportShouldReturnAiVerdictAndStatus() throws Exception {
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
            get("/api/goals/{goalId}/motivation/quote", goalId)
                .header("Authorization", bearer(tokens.accessToken())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.quoteText").isString())
        .andExpect(jsonPath("$.quoteTextRu").isString())
        .andExpect(jsonPath("$.quoteAuthor").isString())
        .andExpect(jsonPath("$.quoteDate").isString());

    mockMvc
        .perform(
            post("/api/goals/{goalId}/motivation/refresh-feed", goalId)
                .header("Authorization", bearer(tokens.accessToken())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.images").isArray())
        .andExpect(
            jsonPath("$.images.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(3)))
        .andExpect(jsonPath("$.quotes").isArray())
        .andExpect(jsonPath("$.quotes.length()").value(3))
        .andExpect(jsonPath("$.quotes[0].quoteText").isString())
        .andExpect(jsonPath("$.quotes[0].quoteTextRu").isString());

    mockMvc
        .perform(
            delete("/api/motivation/{id}", imageId)
                .header("Authorization", bearer(tokens.accessToken())))
        .andExpect(status().isOk());
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
