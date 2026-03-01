package com.pomodoro.app.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pomodoro.app.dto.ErrorResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class RestAuthEntryPoint implements AuthenticationEntryPoint {
  private final ObjectMapper objectMapper;

  public RestAuthEntryPoint(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException, ServletException {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    ErrorResponse error =
        new ErrorResponse("UNAUTHORIZED", "Unauthorized", List.of(), OffsetDateTime.now());
    response.getWriter().write(objectMapper.writeValueAsString(error));
  }
}
