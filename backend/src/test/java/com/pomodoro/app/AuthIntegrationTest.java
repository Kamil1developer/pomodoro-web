package com.pomodoro.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class AuthIntegrationTest extends IntegrationTestSupport {

  @Test
  void registerShouldReturnTokenPair() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "email": "auth1@test.dev",
                                  "password": "password123"
                                }
                                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isString())
        .andExpect(jsonPath("$.refreshToken").isString())
        .andExpect(jsonPath("$.tokenType").value("Bearer"));
  }

  @Test
  void loginWithWrongPasswordShouldFail() throws Exception {
    registerUser("auth2@test.dev", "password123");

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "email": "auth2@test.dev",
                                  "password": "wrongpass"
                                }
                                """))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("APP_ERROR"));
  }

  @Test
  void refreshShouldIssueNewAccessToken() throws Exception {
    Tokens tokens = registerUser("auth3@test.dev", "password123");

    String response =
        mockMvc
            .perform(
                post("/api/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                                {
                                  "refreshToken": "%s"
                                }
                                """
                            .formatted(tokens.refreshToken())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isString())
            .andExpect(jsonPath("$.refreshToken").isString())
            .andReturn()
            .getResponse()
            .getContentAsString();

    JsonNode node = objectMapper.readTree(response);
    assertThat(node.get("accessToken").asText()).isNotBlank();
  }
}
