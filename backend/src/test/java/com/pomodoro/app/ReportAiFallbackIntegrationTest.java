package com.pomodoro.app;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pomodoro.app.service.AiService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;

class ReportAiFallbackIntegrationTest extends IntegrationTestSupport {

  @MockBean private AiService aiService;

  @Test
  void reportServiceFallbackOnAiErrorShouldReturnNeedsMoreInfo() throws Exception {
    Tokens tokens = registerUser("report-ai-fallback@test.dev", "password123");
    Long goalId = createGoal(tokens.accessToken(), "Fallback goal");
    createTask(tokens.accessToken(), goalId, "Написать код сервиса");

    when(aiService.analyzeReportImage(any(), anyString(), any()))
        .thenThrow(new RuntimeException("AI down"));

    MockMultipartFile file =
        new MockMultipartFile("file", "proof.jpg", "image/jpeg", "fake-image-data".getBytes());
    MockMultipartFile comment =
        new MockMultipartFile("comment", "", "text/plain", "написал код".getBytes());

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

  private void createTask(String accessToken, Long goalId, String title) throws Exception {
    mockMvc
        .perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                    "/api/goals/{goalId}/tasks", goalId)
                .header("Authorization", bearer(accessToken))
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
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
