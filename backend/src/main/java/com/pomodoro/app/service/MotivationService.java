package com.pomodoro.app.service;

import com.pomodoro.app.dto.AiDtos;
import com.pomodoro.app.dto.MotivationDtos;
import com.pomodoro.app.entity.Goal;
import com.pomodoro.app.entity.MotivationImage;
import com.pomodoro.app.exception.AppException;
import com.pomodoro.app.repository.MotivationImageRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class MotivationService {
  private final GoalService goalService;
  private final MotivationImageRepository motivationImageRepository;
  private final AiService aiService;
  private final StorageService storageService;

  public MotivationService(
      GoalService goalService,
      MotivationImageRepository motivationImageRepository,
      AiService aiService,
      StorageService storageService) {
    this.goalService = goalService;
    this.motivationImageRepository = motivationImageRepository;
    this.aiService = aiService;
    this.storageService = storageService;
  }

  public MotivationDtos.MotivationResponse generate(Long userId, Long goalId, String styleOptions) {
    Goal goal = goalService.ownedGoal(userId, goalId);
    AiDtos.ImageResult image =
        aiService.generateMotivationImage(
            new AiDtos.GoalContext(goal.getId(), goal.getTitle(), goal.getDescription()),
            styleOptions);
    MotivationImage saved =
        motivationImageRepository.save(
            MotivationImage.builder()
                .goal(goal)
                .imagePath(image.imagePath())
                .prompt(image.usedPrompt())
                .isFavorite(false)
                .createdAt(OffsetDateTime.now())
                .build());
    return toResponse(saved);
  }

  public List<MotivationDtos.MotivationResponse> list(Long userId, Long goalId) {
    goalService.ownedGoal(userId, goalId);
    return motivationImageRepository.findByGoalIdOrderByCreatedAtDesc(goalId).stream()
        .map(this::toResponse)
        .toList();
  }

  public MotivationDtos.MotivationResponse toggleFavorite(
      Long userId, Long imageId, Boolean favorite) {
    MotivationImage image =
        motivationImageRepository
            .findByIdAndGoalUserId(imageId, userId)
            .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Image not found"));
    image.setIsFavorite(favorite != null ? favorite : !image.getIsFavorite());
    return toResponse(motivationImageRepository.save(image));
  }

  public void delete(Long userId, Long imageId) {
    MotivationImage image =
        motivationImageRepository
            .findByIdAndGoalUserId(imageId, userId)
            .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Image not found"));
    motivationImageRepository.delete(image);
    storageService.deletePublicPath(image.getImagePath());
  }

  private MotivationDtos.MotivationResponse toResponse(MotivationImage m) {
    return new MotivationDtos.MotivationResponse(
        m.getId(),
        m.getGoal().getId(),
        m.getImagePath(),
        m.getPrompt(),
        m.getIsFavorite(),
        m.getCreatedAt());
  }
}
