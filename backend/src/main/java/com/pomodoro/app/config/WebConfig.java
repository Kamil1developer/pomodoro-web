package com.pomodoro.app.config;

import java.nio.file.Path;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
  private final AppProperties appProperties;

  public WebConfig(AppProperties appProperties) {
    this.appProperties = appProperties;
  }

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    String location =
        Path.of(appProperties.uploadsDir()).toAbsolutePath().normalize().toUri().toString();
    registry.addResourceHandler("/uploads/**").addResourceLocations(location + "/");
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry
        .addMapping("/api/**")
        .allowedOrigins("http://localhost:5173")
        .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
        .allowCredentials(false);
  }
}
