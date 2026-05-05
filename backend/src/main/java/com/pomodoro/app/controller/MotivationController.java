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

  @GetMapping("/motivation/feed")
  public MotivationDtos.MotivationFeedResponse getFeed(
      @RequestParam(required = false) Long goalId, @RequestParam(required = false) Integer limit) {
    return motivationService.getMotivationFeed(AuthUtil.currentUserId(), goalId, limit);
  }

  @PostMapping("/motivation/images/{imageId}/not-interested")
  public MotivationDtos.FeedbackResponse markNotInterested(@PathVariable Long imageId) {
    return motivationService.markImageNotInteresting(AuthUtil.currentUserId(), imageId);
  }

  @PostMapping("/motivation/cards/{cardId}/not-interested")
  public MotivationDtos.FeedbackResponse markCardNotInterested(@PathVariable Long cardId) {
    return motivationService.markImageNotInteresting(AuthUtil.currentUserId(), cardId);
  }

  @PostMapping("/motivation/images/{imageId}/report")
  public MotivationDtos.FeedbackResponse reportImage(
      @PathVariable Long imageId,
      @RequestBody @Valid MotivationDtos.ReportMotivationImageRequest request) {
    return motivationService.reportImage(
        AuthUtil.currentUserId(), imageId, request.reason(), request.comment());
  }

  @PostMapping("/motivation/cards/{cardId}/report")
  public MotivationDtos.FeedbackResponse reportCard(
      @PathVariable Long cardId,
      @RequestBody @Valid MotivationDtos.ReportMotivationImageRequest request) {
    return motivationService.reportImage(
        AuthUtil.currentUserId(), cardId, request.reason(), request.comment());
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
