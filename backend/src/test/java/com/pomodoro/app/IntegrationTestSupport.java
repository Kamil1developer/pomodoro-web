package com.pomodoro.app;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTestSupport {
  @Autowired protected MockMvc mockMvc;

  @Autowired protected ObjectMapper objectMapper;

  protected Tokens registerUser(String email, String password) throws Exception {
    String payload =
        """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """
            .formatted(email, password);

    String response =
        mockMvc
            .perform(
                post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(payload))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    JsonNode node = objectMapper.readTree(response);
    return new Tokens(node.get("accessToken").asText(), node.get("refreshToken").asText());
  }

  protected Long createGoal(String accessToken, String title) throws Exception {
    String payload =
        """
                {
                  "title": "%s",
                  "description": "test goal",
                  "targetHours": 10
                }
                """
            .formatted(title);

    String response =
        mockMvc
            .perform(
                post("/api/goals")
                    .header("Authorization", bearer(accessToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    return objectMapper.readTree(response).get("id").asLong();
  }

  protected String bearer(String accessToken) {
    return "Bearer " + accessToken;
  }

  protected record Tokens(String accessToken, String refreshToken) {}
}
