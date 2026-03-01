package com.pomodoro.app.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class AppConfig {
  @Bean
  public WebClient openAiWebClient() {
    return WebClient.builder().baseUrl("https://api.openai.com/v1").build();
  }
}
