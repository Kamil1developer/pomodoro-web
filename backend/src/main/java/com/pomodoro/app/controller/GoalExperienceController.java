package com.pomodoro.app.controller;

import com.pomodoro.app.dto.GoalExperienceDtos;
import com.pomodoro.app.service.GoalCommitmentService;
import com.pomodoro.app.util.AuthUtil;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class GoalExperienceController {
  private final GoalCommitmentService goalCommitmentService;

  public GoalExperienceController(GoalCommitmentService goalCommitmentService) {
    this.goalCommitmentService = goalCommitmentService;
  }

  @PostMapping("/goals/{goalId}/commitment")
  public GoalExperienceDtos.CommitmentResponse createCommitment(
      @PathVariable Long goalId,
      @RequestBody @Valid GoalExperienceDtos.CommitmentCreateRequest request) {
    return goalCommitmentService.createCommitment(AuthUtil.currentUserId(), goalId, request);
  }

  @GetMapping("/goals/{goalId}/commitment")
  public GoalExperienceDtos.CommitmentResponse getCommitment(@PathVariable Long goalId) {
    return goalCommitmentService.getCommitment(AuthUtil.currentUserId(), goalId);
  }

  @GetMapping("/goals/{goalId}/today")
  public GoalExperienceDtos.TodayStatusResponse getToday(@PathVariable Long goalId) {
    return goalCommitmentService.getTodayStatus(AuthUtil.currentUserId(), goalId);
  }

  @GetMapping("/goals/{goalId}/forecast")
  public GoalExperienceDtos.ForecastResponse getForecast(@PathVariable Long goalId) {
    return goalCommitmentService.getForecast(AuthUtil.currentUserId(), goalId);
  }

  @GetMapping("/goals/{goalId}/events")
  public List<GoalExperienceDtos.GoalEventResponse> getEvents(@PathVariable Long goalId) {
    return goalCommitmentService.getEvents(AuthUtil.currentUserId(), goalId);
  }

  @GetMapping("/goals/{goalId}/experience")
  public GoalExperienceDtos.GoalExperienceResponse getExperience(@PathVariable Long goalId) {
    return goalCommitmentService.getGoalExperience(AuthUtil.currentUserId(), goalId);
  }

  @GetMapping("/goal-experience")
  public List<GoalExperienceDtos.GoalExperienceResponse> getDashboardExperience() {
    return goalCommitmentService.getDashboardExperience(AuthUtil.currentUserId());
  }
}
