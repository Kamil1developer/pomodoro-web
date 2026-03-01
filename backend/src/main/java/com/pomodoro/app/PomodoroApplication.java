package com.pomodoro.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PomodoroApplication {
  public static void main(String[] args) {
    SpringApplication.run(PomodoroApplication.class, args);
  }
}
