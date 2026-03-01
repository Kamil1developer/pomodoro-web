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
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class MotivationService {
  private static final int PIN_HOURS = 24;

  private static final List<QuoteItem> QUOTES =
      List.of(
          new QuoteItem(
              "Discipline is choosing between what you want now and what you want most.",
              "Дисциплина — это выбор между тем, чего вы хотите сейчас, и тем, чего хотите больше всего.",
              "Abraham Lincoln"),
          new QuoteItem(
              "Success is the sum of small efforts, repeated day in and day out.",
              "Успех — это сумма маленьких усилий, повторяемых изо дня в день.",
              "Robert Collier"),
          new QuoteItem(
              "We are what we repeatedly do. Excellence, then, is a habit.",
              "Мы есть то, что постоянно делаем. Поэтому совершенство — это привычка.",
              "Aristotle"),
          new QuoteItem(
              "The secret of getting ahead is getting started.",
              "Секрет продвижения вперед в том, чтобы начать.",
              "Mark Twain"),
          new QuoteItem(
              "Action is the foundational key to all success.",
              "Действие — это фундаментальный ключ к любому успеху.",
              "Pablo Picasso"),
          new QuoteItem(
              "What gets scheduled gets done, and what gets done changes your life.",
              "То, что запланировано, выполняется; а выполненное меняет твою жизнь.",
              "Robin Sharma"),
          new QuoteItem(
              "Small daily improvements over time lead to stunning results.",
              "Маленькие ежедневные улучшения со временем приводят к впечатляющим результатам.",
              "Robin Sharma"),
          new QuoteItem(
              "The future depends on what you do today.",
              "Будущее зависит от того, что ты делаешь сегодня.",
              "Mahatma Gandhi"),
          new QuoteItem(
              "Do not wait to strike till the iron is hot; make it hot by striking.",
              "Не жди, пока железо раскалится; раскаляй его ударами.",
              "William Butler Yeats"),
          new QuoteItem(
              "Well done is better than well said.",
              "Хорошо сделано лучше, чем хорошо сказано.",
              "Benjamin Franklin"));

  private static final List<String> FEED_STYLES =
      List.of(
          "cinematic success poster",
          "clean productivity workspace",
          "athletic discipline mood",
          "calm morning focus aesthetic",
          "bold achievement collage",
          "minimalist high-contrast motivation");

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
    QuoteItem quoteItem = pickQuote(goal, quoteDate);
    motivationQuoteRepository
        .findByGoalIdAndQuoteDate(goal.getId(), quoteDate)
        .orElseGet(
            () ->
                motivationQuoteRepository.save(
                    MotivationQuote.builder()
                        .goal(goal)
                        .quoteText(quoteItem.text())
                        .quoteAuthor(quoteItem.author())
                        .quoteDate(quoteDate)
                        .createdAt(OffsetDateTime.now())
                        .build()));
  }

  public MotivationDtos.DailyQuoteResponse getDailyQuote(Long userId, Long goalId) {
    Goal goal = goalService.ownedGoal(userId, goalId);
    return getOrCreateDailyQuote(goal, LocalDate.now());
  }

  public void ensureDailyQuotesForAllGoals(List<Goal> goals, LocalDate date) {
    for (Goal goal : goals) {
      ensureDailyQuoteForGoal(goal, date);
    }
  }

  public MotivationDtos.FeedRefreshResponse refreshFeed(Long userId, Long goalId) {
    Goal goal = goalService.ownedGoal(userId, goalId);
    saveImage(goal, pickFeedStyle(), MotivationImageSource.AUTO);
    return new MotivationDtos.FeedRefreshResponse(
        listByGoal(goal, OffsetDateTime.now()), getOrCreateDailyQuote(goal, LocalDate.now()));
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
    Goal goal = goalService.ownedGoal(userId, goalId);
    return listByGoal(goal, OffsetDateTime.now());
  }

  private List<MotivationDtos.MotivationResponse> listByGoal(Goal goal, OffsetDateTime now) {
    return motivationImageRepository.findByGoalIdOrderByCreatedAtDesc(goal.getId()).stream()
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
        resolveQuoteTranslation(quote.getQuoteText(), quote.getQuoteAuthor()),
        quote.getQuoteAuthor(),
        quote.getQuoteDate().toString());
  }

  private MotivationDtos.DailyQuoteResponse getOrCreateDailyQuote(Goal goal, LocalDate date) {
    ensureDailyQuoteForGoal(goal, date);
    MotivationQuote quote =
        motivationQuoteRepository
            .findByGoalIdAndQuoteDate(goal.getId(), date)
            .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Quote not found"));
    return toQuoteResponse(quote);
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

  private String resolveQuoteTranslation(String quoteText, String quoteAuthor) {
    return QUOTES.stream()
        .filter(item -> item.text().equals(quoteText) && item.author().equals(quoteAuthor))
        .map(QuoteItem::textRu)
        .findFirst()
        .orElse(quoteText);
  }

  private String pickFeedStyle() {
    return FEED_STYLES.get(ThreadLocalRandom.current().nextInt(FEED_STYLES.size()));
  }

  private record QuoteItem(String text, String textRu, String author) {}
}
