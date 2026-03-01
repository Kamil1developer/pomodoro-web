package com.pomodoro.app.controller;

import com.pomodoro.app.dto.MotivationDtos;
import com.pomodoro.app.service.MotivationService;
import com.pomodoro.app.util.AuthUtil;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class MotivationController {
  private final MotivationService motivationService;

  public MotivationController(MotivationService motivationService) {
    this.motivationService = motivationService;
  }

  @PostMapping("/goals/{goalId}/motivation/generate")
  public MotivationDtos.MotivationResponse generate(
      @PathVariable Long goalId,
      @RequestBody(required = false) MotivationDtos.GenerateRequest request) {
    return motivationService.generate(
        AuthUtil.currentUserId(), goalId, request != null ? request.styleOptions() : null);
  }

  @GetMapping("/goals/{goalId}/motivation")
  public List<MotivationDtos.MotivationResponse> list(@PathVariable Long goalId) {
    return motivationService.list(AuthUtil.currentUserId(), goalId);
  }

  @GetMapping("/goals/{goalId}/motivation/quote")
  public MotivationDtos.DailyQuoteResponse dailyQuote(@PathVariable Long goalId) {
    return motivationService.getDailyQuote(AuthUtil.currentUserId(), goalId);
  }

  @PostMapping("/goals/{goalId}/motivation/refresh-feed")
  public MotivationDtos.FeedRefreshResponse refreshFeed(@PathVariable Long goalId) {
    return motivationService.refreshFeed(AuthUtil.currentUserId(), goalId);
  }

  @PatchMapping("/motivation/{imgId}/favorite")
  public MotivationDtos.MotivationResponse favorite(
      @PathVariable Long imgId, @RequestBody @Valid MotivationDtos.FavoriteRequest request) {
    return motivationService.toggleFavorite(AuthUtil.currentUserId(), imgId, request.isFavorite());
  }

  @DeleteMapping("/motivation/{imgId}")
  public void delete(@PathVariable Long imgId) {
    motivationService.delete(AuthUtil.currentUserId(), imgId);
  }
}
