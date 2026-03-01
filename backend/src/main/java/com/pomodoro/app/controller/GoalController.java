package com.pomodoro.app.controller;

import com.pomodoro.app.dto.GoalDtos;
import com.pomodoro.app.service.GoalService;
import com.pomodoro.app.util.AuthUtil;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class GoalController {
  private final GoalService goalService;

  public GoalController(GoalService goalService) {
    this.goalService = goalService;
  }

  @GetMapping("/goals")
  public List<GoalDtos.GoalResponse> getGoals() {
    return goalService.getGoals(AuthUtil.currentUserId());
  }

  @GetMapping("/goals/{id}")
  public GoalDtos.GoalResponse getGoal(@PathVariable Long id) {
    return goalService.getGoal(AuthUtil.currentUserId(), id);
  }

  @PostMapping("/goals")
  public GoalDtos.GoalResponse createGoal(@RequestBody @Valid GoalDtos.GoalCreateRequest request) {
    return goalService.createGoal(AuthUtil.currentUserId(), request);
  }

  @PutMapping("/goals/{id}")
  public GoalDtos.GoalResponse updateGoal(
      @PathVariable Long id, @RequestBody @Valid GoalDtos.GoalUpdateRequest request) {
    return goalService.updateGoal(AuthUtil.currentUserId(), id, request);
  }

  @DeleteMapping("/goals/{id}")
  public void deleteGoal(@PathVariable Long id) {
    goalService.deleteGoal(AuthUtil.currentUserId(), id);
  }

  @GetMapping("/goals/{id}/tasks")
  public List<GoalDtos.TaskResponse> getTasks(@PathVariable Long id) {
    return goalService.getTasks(AuthUtil.currentUserId(), id);
  }

  @PostMapping("/goals/{id}/tasks")
  public GoalDtos.TaskResponse createTask(
      @PathVariable Long id, @RequestBody @Valid GoalDtos.TaskCreateRequest request) {
    return goalService.createTask(AuthUtil.currentUserId(), id, request);
  }

  @PutMapping("/goals/{goalId}/tasks/{taskId}")
  public GoalDtos.TaskResponse updateTask(
      @PathVariable Long goalId,
      @PathVariable Long taskId,
      @RequestBody @Valid GoalDtos.TaskUpdateRequest request) {
    return goalService.updateTask(AuthUtil.currentUserId(), goalId, taskId, request);
  }

  @DeleteMapping("/goals/{goalId}/tasks/{taskId}")
  public void deleteTask(@PathVariable Long goalId, @PathVariable Long taskId) {
    goalService.deleteTask(AuthUtil.currentUserId(), goalId, taskId);
  }

  @GetMapping("/goals/{id}/progress")
  public GoalDtos.GoalProgressResponse progress(@PathVariable Long id) {
    return goalService.getProgress(AuthUtil.currentUserId(), id);
  }

  @GetMapping("/goals/{id}/stats")
  public GoalDtos.GoalStatsResponse stats(@PathVariable Long id) {
    return goalService.getStats(AuthUtil.currentUserId(), id);
  }
}
