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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class MotivationService {
  private static final Logger log = LoggerFactory.getLogger(MotivationService.class);
  private static final int PIN_HOURS = 24;
  private static final int FEED_IMAGE_BATCH_SIZE = 3;
  private static final int FEED_QUOTE_BATCH_SIZE = 3;

  private static final List<String> SPORT_MARKERS =
      List.of("спорт", "sport", "gym", "fitness", "workout", "run", "кардио", "трен");
  private static final List<String> STUDY_MARKERS =
      List.of("учеб", "study", "exam", "course", "university", "диплом", "language");
  private static final List<String> CODE_MARKERS =
      List.of("java", "programming", "code", "developer", "backend", "frontend", "разработ");

  private static final List<QuoteItem> SPORT_QUOTES =
      List.of(
          new QuoteItem(
              "The only bad workout is the one that didn't happen.",
              "Плохая тренировка только та, которой не было.",
              "Unknown"),
          new QuoteItem(
              "Strength does not come from what you can do. It comes from overcoming what you once thought you couldn't.",
              "Сила приходит не от того, что ты уже можешь, а от преодоления того, что раньше казалось невозможным.",
              "Rikki Rogers"),
          new QuoteItem(
              "Take care of your body. It's the only place you have to live.",
              "Заботься о своем теле. Это единственное место, где тебе жить.",
              "Jim Rohn"),
          new QuoteItem(
              "Discipline is doing what needs to be done, even if you don't want to.",
              "Дисциплина — делать то, что нужно, даже когда не хочется.",
              "Unknown"));

  private static final List<QuoteItem> STUDY_QUOTES =
      List.of(
          new QuoteItem(
              "Success is the sum of small efforts, repeated day in and day out.",
              "Успех — это сумма маленьких усилий, повторяемых изо дня в день.",
              "Robert Collier"),
          new QuoteItem(
              "An investment in knowledge always pays the best interest.",
              "Инвестиции в знания приносят наибольшую прибыль.",
              "Benjamin Franklin"),
          new QuoteItem(
              "Learning never exhausts the mind.",
              "Обучение никогда не истощает разум.",
              "Leonardo da Vinci"),
          new QuoteItem(
              "The secret of getting ahead is getting started.",
              "Секрет продвижения вперед в том, чтобы начать.",
              "Mark Twain"));

  private static final List<QuoteItem> CODE_QUOTES =
      List.of(
          new QuoteItem(
              "First, solve the problem. Then, write the code.",
              "Сначала реши задачу. Потом пиши код.",
              "John Johnson"),
          new QuoteItem(
              "Simplicity is the soul of efficiency.",
              "Простота — душа эффективности.",
              "Austin Freeman"),
          new QuoteItem(
              "Code is like humor. When you have to explain it, it's bad.",
              "Код как юмор: если его надо объяснять, он плох.",
              "Cory House"),
          new QuoteItem(
              "The future depends on what you do today.",
              "Будущее зависит от того, что ты делаешь сегодня.",
              "Mahatma Gandhi"));

  private static final List<QuoteItem> GENERAL_QUOTES =
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

  private static final List<String> SPORT_TERMS =
      List.of(
          "sport gym workout fitness athlete training motivation",
          "running training discipline healthy lifestyle",
          "sport motivation progress strength body transformation");
  private static final List<String> STUDY_TERMS =
      List.of(
          "study learning student books focus motivation",
          "exam preparation concentration productivity education",
          "university success deep work discipline");
  private static final List<String> CODE_TERMS =
      List.of(
          "coding programmer developer office laptop productivity",
          "software engineering backend frontend career growth",
          "java programming deep focus clean code");
  private static final List<String> GENERAL_TERMS =
      List.of(
          "motivation success focus progress discipline",
          "goal achievement consistency mindset",
          "productivity habit building personal growth");

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
    for (String style : pickFeedStyles(goal)) {
      try {
        saveImage(goal, style, MotivationImageSource.AUTO);
      } catch (Exception e) {
        log.warn(
            "Не удалось обновить мотивационную картинку для цели {}: {}",
            goal.getId(),
            e.getMessage());
      }
    }
    return new MotivationDtos.FeedRefreshResponse(
        listByGoal(goal, OffsetDateTime.now()), pickFeedQuotes(goal, FEED_QUOTE_BATCH_SIZE));
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
    List<QuoteItem> pool = pickQuotePool(goal);
    int index = Math.floorMod(seed, pool.size());
    return pool.get(index);
  }

  private String resolveQuoteTranslation(String quoteText, String quoteAuthor) {
    return allQuotes().stream()
        .filter(item -> item.text().equals(quoteText) && item.author().equals(quoteAuthor))
        .map(QuoteItem::textRu)
        .findFirst()
        .orElse(quoteText);
  }

  private List<String> pickFeedStyles(Goal goal) {
    List<String> pool =
        switch (detectTheme(goal)) {
          case SPORT -> SPORT_TERMS;
          case STUDY -> STUDY_TERMS;
          case CODE -> CODE_TERMS;
          default -> GENERAL_TERMS;
        };
    if (pool.isEmpty()) {
      return List.of("motivation success focus progress discipline");
    }
    List<String> shuffled = new ArrayList<>(pool);
    Collections.shuffle(shuffled);
    List<String> styles = new ArrayList<>();
    for (int i = 0; i < FEED_IMAGE_BATCH_SIZE; i++) {
      styles.add(shuffled.get(i % shuffled.size()));
    }
    return styles;
  }

  private List<MotivationDtos.DailyQuoteResponse> pickFeedQuotes(Goal goal, int limit) {
    List<QuoteItem> pool = pickQuotePool(goal);
    if (pool.isEmpty()) {
      return List.of();
    }
    int size = Math.min(Math.max(limit, 1), pool.size());
    int start = ThreadLocalRandom.current().nextInt(pool.size());
    List<MotivationDtos.DailyQuoteResponse> quotes = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      QuoteItem item = pool.get((start + i) % pool.size());
      quotes.add(
          new MotivationDtos.DailyQuoteResponse(
              null,
              goal.getId(),
              item.text(),
              item.textRu(),
              item.author(),
              LocalDate.now().toString()));
    }
    return quotes;
  }

  private List<QuoteItem> pickQuotePool(Goal goal) {
    return switch (detectTheme(goal)) {
      case SPORT -> SPORT_QUOTES;
      case STUDY -> STUDY_QUOTES;
      case CODE -> CODE_QUOTES;
      default -> GENERAL_QUOTES;
    };
  }

  private GoalTheme detectTheme(Goal goal) {
    String text =
        (goal.getTitle() + " " + (goal.getDescription() == null ? " " : goal.getDescription()))
            .toLowerCase();
    if (containsAny(text, SPORT_MARKERS)) {
      return GoalTheme.SPORT;
    }
    if (containsAny(text, CODE_MARKERS)) {
      return GoalTheme.CODE;
    }
    if (containsAny(text, STUDY_MARKERS)) {
      return GoalTheme.STUDY;
    }
    return GoalTheme.GENERAL;
  }

  private boolean containsAny(String text, List<String> markers) {
    for (String marker : markers) {
      if (text.contains(marker)) {
        return true;
      }
    }
    return false;
  }

  private List<QuoteItem> allQuotes() {
    List<QuoteItem> all = new ArrayList<>();
    all.addAll(SPORT_QUOTES);
    all.addAll(STUDY_QUOTES);
    all.addAll(CODE_QUOTES);
    all.addAll(GENERAL_QUOTES);
    return all;
  }

  private enum GoalTheme {
    SPORT,
    STUDY,
    CODE,
    GENERAL
  }

  private record QuoteItem(String text, String textRu, String author) {}
}
