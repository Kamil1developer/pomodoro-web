package com.pomodoro.app.controller;

import com.pomodoro.app.dto.GoalDtos;
import com.pomodoro.app.service.FocusSessionService;
import com.pomodoro.app.util.AuthUtil;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/goals/{goalId}/focus")
public class FocusController {
  private final FocusSessionService focusSessionService;

  public FocusController(FocusSessionService focusSessionService) {
    this.focusSessionService = focusSessionService;
  }

  @PostMapping("/start")
  public GoalDtos.FocusSessionResponse start(@PathVariable Long goalId) {
    return focusSessionService.start(AuthUtil.currentUserId(), goalId);
  }

  @PostMapping("/stop")
  public GoalDtos.FocusSessionResponse stop(@PathVariable Long goalId) {
    return focusSessionService.stop(AuthUtil.currentUserId(), goalId);
  }

  @GetMapping
  public List<GoalDtos.FocusSessionResponse> list(@PathVariable Long goalId) {
    return focusSessionService.list(AuthUtil.currentUserId(), goalId);
  }
}
