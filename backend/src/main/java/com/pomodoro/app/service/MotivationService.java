package com.pomodoro.app.service;

import com.pomodoro.app.dto.AiDtos;
import com.pomodoro.app.dto.MotivationDtos;
import com.pomodoro.app.entity.Goal;
import com.pomodoro.app.entity.MotivationImage;
import com.pomodoro.app.entity.MotivationQuote;
import com.pomodoro.app.enums.MotivationImageSource;
import com.pomodoro.app.exception.AppException;
import com.pomodoro.app.repository.MotivationImageRepository;
import com.pomodoro.app.repository.MotivationQuoteRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class MotivationService {
  private static final int PIN_HOURS = 24;

  private static final List<QuoteItem> QUOTES =
      List.of(
          new QuoteItem(
              "Discipline is choosing between what you want now and what you want most.",
              "Abraham Lincoln"),
          new QuoteItem(
              "Success is the sum of small efforts, repeated day in and day out.", "Robert Collier"),
          new QuoteItem("We are what we repeatedly do. Excellence, then, is a habit.", "Aristotle"),
          new QuoteItem("The secret of getting ahead is getting started.", "Mark Twain"),
          new QuoteItem("Action is the foundational key to all success.", "Pablo Picasso"),
          new QuoteItem(
              "What gets scheduled gets done, and what gets done changes your life.",
              "Robin Sharma"),
          new QuoteItem(
              "Small daily improvements over time lead to stunning results.", "Robin Sharma"),
          new QuoteItem("The future depends on what you do today.", "Mahatma Gandhi"),
          new QuoteItem("Do not wait to strike till the iron is hot; make it hot by striking.",
              "William Butler Yeats"),
          new QuoteItem("Well done is better than well said.", "Benjamin Franklin"));

  private final GoalService goalService;
  private final MotivationImageRepository motivationImageRepository;
  private final MotivationQuoteRepository motivationQuoteRepository;
  private final AiService aiService;
  private final StorageService storageService;

  public MotivationService(
      GoalService goalService,
      MotivationImageRepository motivationImageRepository,
      MotivationQuoteRepository motivationQuoteRepository,
      AiService aiService,
      StorageService storageService) {
    this.goalService = goalService;
    this.motivationImageRepository = motivationImageRepository;
    this.motivationQuoteRepository = motivationQuoteRepository;
    this.aiService = aiService;
    this.storageService = storageService;
  }

  public MotivationDtos.MotivationResponse generate(Long userId, Long goalId, String styleOptions) {
    Goal goal = goalService.ownedGoal(userId, goalId);
    return saveImage(goal, styleOptions, MotivationImageSource.MANUAL);
  }

  public MotivationDtos.MotivationResponse generateAutoForGoal(Goal goal) {
    String style = "cinematic, future success vision, productivity";
    return saveImage(goal, style, MotivationImageSource.AUTO);
  }

  public void ensureDailyQuoteForGoal(Goal goal, LocalDate quoteDate) {
    motivationQuoteRepository
        .findByGoalIdAndQuoteDate(goal.getId(), quoteDate)
        .orElseGet(
            () ->
                motivationQuoteRepository.save(
                    MotivationQuote.builder()
                        .goal(goal)
                        .quoteText(pickQuote(goal, quoteDate).text())
                        .quoteAuthor(pickQuote(goal, quoteDate).author())
                        .quoteDate(quoteDate)
                        .createdAt(OffsetDateTime.now())
                        .build()));
  }

  public MotivationDtos.DailyQuoteResponse getDailyQuote(Long userId, Long goalId) {
    Goal goal = goalService.ownedGoal(userId, goalId);
    LocalDate today = LocalDate.now();
    ensureDailyQuoteForGoal(goal, today);
    MotivationQuote quote =
        motivationQuoteRepository
            .findByGoalIdAndQuoteDate(goal.getId(), today)
            .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Quote not found"));
    return toQuoteResponse(quote);
  }

  public void ensureDailyQuotesForAllGoals(List<Goal> goals, LocalDate date) {
    for (Goal goal : goals) {
      ensureDailyQuoteForGoal(goal, date);
    }
  }

  private MotivationDtos.MotivationResponse saveImage(
      Goal goal, String styleOptions, MotivationImageSource source) {
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
                .generatedBy(source)
                .favoritedAt(null)
                .createdAt(OffsetDateTime.now())
                .build());
    return toResponse(saved, OffsetDateTime.now());
  }

  public List<MotivationDtos.MotivationResponse> list(Long userId, Long goalId) {
    goalService.ownedGoal(userId, goalId);
    OffsetDateTime now = OffsetDateTime.now();
    return motivationImageRepository.findByGoalIdOrderByCreatedAtDesc(goalId).stream()
        .sorted(
            Comparator.comparing((MotivationImage image) -> isPinned(image, now))
                .reversed()
                .thenComparing(MotivationImage::getCreatedAt, Comparator.reverseOrder()))
        .map(image -> toResponse(image, now))
        .toList();
  }

  public MotivationDtos.MotivationResponse toggleFavorite(
      Long userId, Long imageId, Boolean favorite) {
    MotivationImage image =
        motivationImageRepository
            .findByIdAndGoalUserId(imageId, userId)
            .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Image not found"));
    boolean nextValue = favorite != null ? favorite : !image.getIsFavorite();
    image.setIsFavorite(nextValue);
    image.setFavoritedAt(nextValue ? OffsetDateTime.now() : null);
    return toResponse(motivationImageRepository.save(image), OffsetDateTime.now());
  }

  public void delete(Long userId, Long imageId) {
    MotivationImage image =
        motivationImageRepository
            .findByIdAndGoalUserId(imageId, userId)
            .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Image not found"));
    motivationImageRepository.delete(image);
    storageService.deletePublicPath(image.getImagePath());
  }

  private MotivationDtos.MotivationResponse toResponse(MotivationImage m, OffsetDateTime now) {
    OffsetDateTime pinnedUntil = pinnedUntil(m);
    return new MotivationDtos.MotivationResponse(
        m.getId(),
        m.getGoal().getId(),
        m.getImagePath(),
        m.getPrompt(),
        m.getIsFavorite(),
        m.getGeneratedBy(),
        m.getFavoritedAt(),
        pinnedUntil,
        isPinned(m, now),
        m.getCreatedAt());
  }

  private MotivationDtos.DailyQuoteResponse toQuoteResponse(MotivationQuote quote) {
    return new MotivationDtos.DailyQuoteResponse(
        quote.getId(),
        quote.getGoal().getId(),
        quote.getQuoteText(),
        quote.getQuoteAuthor(),
        quote.getQuoteDate().toString());
  }

  private OffsetDateTime pinnedUntil(MotivationImage image) {
    if (!Boolean.TRUE.equals(image.getIsFavorite()) || image.getFavoritedAt() == null) {
      return null;
    }
    return image.getFavoritedAt().plusHours(PIN_HOURS);
  }

  private boolean isPinned(MotivationImage image, OffsetDateTime now) {
    OffsetDateTime pinnedUntil = pinnedUntil(image);
    return pinnedUntil != null && pinnedUntil.isAfter(now);
  }

  private QuoteItem pickQuote(Goal goal, LocalDate quoteDate) {
    int seed = Long.hashCode(goal.getId() + quoteDate.toEpochDay());
    int index = Math.floorMod(seed, QUOTES.size());
    return QUOTES.get(index);
  }

  private record QuoteItem(String text, String author) {}
}
