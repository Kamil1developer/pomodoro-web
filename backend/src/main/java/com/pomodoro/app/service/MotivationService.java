package com.pomodoro.app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pomodoro.app.config.AppProperties;
import com.pomodoro.app.dto.GoalExperienceDtos;
import com.pomodoro.app.dto.MotivationDtos;
import com.pomodoro.app.entity.Goal;
import com.pomodoro.app.entity.MotivationImage;
import com.pomodoro.app.entity.MotivationImageFeedback;
import com.pomodoro.app.entity.MotivationQuote;
import com.pomodoro.app.entity.User;
import com.pomodoro.app.enums.MotivationImageFeedbackType;
import com.pomodoro.app.enums.MotivationImageReportReason;
import com.pomodoro.app.enums.MotivationImageSource;
import com.pomodoro.app.exception.AppException;
import com.pomodoro.app.repository.MotivationImageFeedbackRepository;
import com.pomodoro.app.repository.MotivationImageRepository;
import com.pomodoro.app.repository.MotivationQuoteRepository;
import com.pomodoro.app.repository.UserRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Service
public class MotivationService {
  private static final Logger log = LoggerFactory.getLogger(MotivationService.class);
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final int PIN_HOURS = 24;
  private static final int FEED_IMAGE_BATCH_SIZE = 3;
  private static final int FEED_QUOTE_BATCH_SIZE = 3;
  private static final int DEFAULT_FEED_LIMIT = 10;
  private static final int GLOBAL_REPORT_THRESHOLD = 3;

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
              "Pablo Picasso"));

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

  private static final List<String> SPORT_FALLBACK_PHOTOS =
      List.of(
          "https://images.unsplash.com/photo-1517836357463-d25dfeac3438?auto=format&fit=crop&w=1000&q=85",
          "https://images.unsplash.com/photo-1518611012118-696072aa579a?auto=format&fit=crop&w=1000&q=85",
          "https://images.unsplash.com/photo-1461896836934-ffe607ba8211?auto=format&fit=crop&w=1000&q=85",
          "https://images.unsplash.com/photo-1526506118085-60ce8714f8c5?auto=format&fit=crop&w=1000&q=85",
          "https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?auto=format&fit=crop&w=1000&q=85");
  private static final List<String> STUDY_FALLBACK_PHOTOS =
      List.of(
          "https://images.unsplash.com/photo-1513258496099-48168024aec0?auto=format&fit=crop&w=1000&q=85",
          "https://images.unsplash.com/photo-1522202176988-66273c2fd55f?auto=format&fit=crop&w=1000&q=85",
          "https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?auto=format&fit=crop&w=1000&q=85",
          "https://images.unsplash.com/photo-1497633762265-9d179a990aa6?auto=format&fit=crop&w=1000&q=85",
          "https://images.unsplash.com/photo-1503676260728-1c00da094a0b?auto=format&fit=crop&w=1000&q=85");
  private static final List<String> CODE_FALLBACK_PHOTOS =
      List.of(
          "https://images.unsplash.com/photo-1515879218367-8466d910aaa4?auto=format&fit=crop&w=1000&q=85",
          "https://images.unsplash.com/photo-1498050108023-c5249f4df085?auto=format&fit=crop&w=1000&q=85",
          "https://images.unsplash.com/photo-1555066931-4365d14bab8c?auto=format&fit=crop&w=1000&q=85",
          "https://images.unsplash.com/photo-1531482615713-2afd69097998?auto=format&fit=crop&w=1000&q=85",
          "https://images.unsplash.com/photo-1461749280684-dccba630e2f6?auto=format&fit=crop&w=1000&q=85");
  private static final List<String> GENERAL_FALLBACK_PHOTOS =
      List.of(
          "https://images.unsplash.com/photo-1499750310107-5fef28a66643?auto=format&fit=crop&w=1000&q=85",
          "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=1000&q=85",
          "https://images.unsplash.com/photo-1506784983877-45594efa4cbe?auto=format&fit=crop&w=1000&q=85",
          "https://images.unsplash.com/photo-1484480974693-6ca0a78fb36b?auto=format&fit=crop&w=1000&q=85",
          "https://images.unsplash.com/photo-1497366754035-f200968a6e72?auto=format&fit=crop&w=1000&q=85");

  private static final List<FallbackCardTemplate> SPORT_FALLBACK_CARDS =
      List.of(
          new FallbackCardTemplate(
              "Собери инерцию",
              "Даже короткая тренировка укрепляет ритм и не даёт цели рассыпаться.",
              "Один подход сегодня важнее идеального плана на завтра."),
          new FallbackCardTemplate(
              "Дисциплина в движении",
              "Регулярность создаёт тело и уверенность быстрее, чем редкие рывки.",
              "Сделай маленький спортивный шаг и зафиксируй результат."),
          new FallbackCardTemplate(
              "Не жди идеального момента",
              "Фокус на процессе помогает вернуться в темп, даже если мотивации мало.",
              "20 минут движения уже работают на цель."),
          new FallbackCardTemplate(
              "Сильный темп",
              "Серия держится на простом правиле: не пропускай два дня подряд.",
              "Сохрани streak одной короткой сессией."),
          new FallbackCardTemplate(
              "Работай на привычку",
              "Тело любит повторение. Повторение любит ясный следующий шаг.",
              "Начни с разминки и дойди до видимого результата."),
          new FallbackCardTemplate(
              "Финиш важнее старта",
              "День засчитывается, когда есть и работа, и доказательство результата.",
              "После тренировки не забудь фото-отчёт."));

  private static final List<FallbackCardTemplate> STUDY_FALLBACK_CARDS =
      List.of(
          new FallbackCardTemplate(
              "Учёба любит ритм",
              "Короткая сессия с понятной задачей почти всегда лучше ожидания вдохновения.",
              "Открой материал и сделай один законченный шаг."),
          new FallbackCardTemplate(
              "Понимание растёт слоями",
              "Каждый конспект, упражнение и повторение делает цель ближе.",
              "Закрой одну учебную задачу сегодня и закрепи её отчётом."),
          new FallbackCardTemplate(
              "Глубина через регулярность",
              "Не пытайся осилить всё сразу. Двигайся кусками, но каждый день.",
              "Ещё одна Pomodoro-сессия сегодня сохранит темп."),
          new FallbackCardTemplate(
              "Сначала маленький результат",
              "Одна страница конспекта или один выполненный блок уже меняют день.",
              "Сделай небольшой, но доказуемый прогресс."),
          new FallbackCardTemplate(
              "Фокус важнее шума",
              "Не оценивай день по настроению. Оцени по завершённому шагу.",
              "Выбери самую понятную учебную задачу и закрой её."),
          new FallbackCardTemplate(
              "Знание любит доказательства",
              "Если задача практическая, нужен видимый результат: конспект, решение или документ.",
              "Подготовь фото-отчёт так, чтобы был виден итог."));

  private static final List<FallbackCardTemplate> CODE_FALLBACK_CARDS =
      List.of(
          new FallbackCardTemplate(
              "Код появляется из маленьких шагов",
              "Не жди большого прорыва. Напиши один метод, поправь один баг, проверь один сценарий.",
              "Сделай один технический результат и зафиксируй его."),
          new FallbackCardTemplate(
              "Практика сильнее просмотра",
              "Видео и статьи помогают, но цель движется вперёд, когда появляется работающий артефакт.",
              "Открой IDE и доведи одну часть до результата."),
          new FallbackCardTemplate(
              "Доводи до доказательства",
              "Код, тесты, структура проекта или документ — всё это лучше абстрактного «я занимался».",
              "После сессии подготовь фото с видимым результатом."),
          new FallbackCardTemplate(
              "Один коммит мышления",
              "Разработка любит ясную границу: что именно будет завершено за ближайшие 25 минут.",
              "Сформулируй один маленький deliverable и закрой его."),
          new FallbackCardTemplate(
              "Темп важнее идеальности",
              "Не пытайся сразу сделать идеально. Сделай работающий шаг и улучши потом.",
              "Сегодня достаточно одного законченного блока по проекту."),
          new FallbackCardTemplate(
              "Техническая дисциплина",
              "Когда внимание расползается, возвращайся к самому понятному следующему действию.",
              "Запусти короткую фокус-сессию и заверши один конкретный кусок работы."));

  private static final List<FallbackCardTemplate> GENERAL_FALLBACK_CARDS =
      List.of(
          new FallbackCardTemplate(
              "Двигай цель каждый день",
              "Большая цель складывается из маленьких повторяемых действий.",
              "Сделай ещё один короткий шаг сегодня."),
          new FallbackCardTemplate(
              "Темп важнее настроения",
              "Не нужно ждать идеального состояния, чтобы сделать полезный шаг.",
              "Запусти одну Pomodoro-сессию и посмотри, что изменится."),
          new FallbackCardTemplate(
              "Сохрани серию",
              "Даже короткая работа сегодня поддерживает дисциплину и streak.",
              "Закрой минимальный, но реальный прогресс."),
          new FallbackCardTemplate(
              "Прогресс любит доказательства",
              "Когда результат виден, его проще признать и продолжить дальше.",
              "После работы не забудь подтвердить её отчётом."),
          new FallbackCardTemplate(
              "Следующий шаг важнее тревоги",
              "Если неясно, что делать, выбери самое маленькое полезное действие.",
              "Один завершённый шаг уже меняет день."),
          new FallbackCardTemplate(
              "Личное обязательство работает",
              "Дневная норма существует не для давления, а для устойчивого движения.",
              "Вернись к цели и закрой хотя бы часть нормы."));

  private final GoalService goalService;
  private final MotivationImageRepository motivationImageRepository;
  private final MotivationImageFeedbackRepository motivationImageFeedbackRepository;
  private final MotivationQuoteRepository motivationQuoteRepository;
  private final AiService aiService;
  private final StorageService storageService;
  private final UserRepository userRepository;
  private final GoalCommitmentService goalCommitmentService;
  private final WebClient webImageWebClient;
  private final Map<String, FeedRefreshState> feedRefreshStates = new ConcurrentHashMap<>();

  public MotivationService(
      GoalService goalService,
      MotivationImageRepository motivationImageRepository,
      MotivationImageFeedbackRepository motivationImageFeedbackRepository,
      MotivationQuoteRepository motivationQuoteRepository,
      AiService aiService,
      StorageService storageService,
      UserRepository userRepository,
      GoalCommitmentService goalCommitmentService,
      AppProperties appProperties,
      WebClient.Builder webClientBuilder) {
    this.goalService = goalService;
    this.motivationImageRepository = motivationImageRepository;
    this.motivationImageFeedbackRepository = motivationImageFeedbackRepository;
    this.motivationQuoteRepository = motivationQuoteRepository;
    this.aiService = aiService;
    this.storageService = storageService;
    this.userRepository = userRepository;
    this.goalCommitmentService = goalCommitmentService;
    this.webImageWebClient =
        webClientBuilder
            .clone()
            .baseUrl(appProperties.ai().webImageApiUrl())
            .clientConnector(
                new ReactorClientHttpConnector(HttpClient.create().followRedirect(true)))
            .build();
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
    ensureFeedCards(goal, DEFAULT_FEED_LIMIT + 6);
    FeedRefreshState state = nextFeedRefreshState(userId, goal.getId());
    return new MotivationDtos.FeedRefreshResponse(
        listByGoal(goal, OffsetDateTime.now()),
        pickFeedQuotes(goal, FEED_QUOTE_BATCH_SIZE),
        state.refreshSessionId(),
        state.feedVersion(),
        state.generatedAt(),
        "Новые изображения временно не найдены, мы обновили порядок и цитаты.");
  }

  public MotivationDtos.MotivationFeedResponse getMotivationFeed(
      Long userId, Long goalId, Integer limit) {
    Goal goal = resolveGoalForFeed(userId, goalId);
    int normalizedLimit = normalizeLimit(limit);
    FeedRefreshState state = currentFeedRefreshState(userId, goal.getId());
    ensureDailyQuoteForGoal(goal, LocalDate.now());
    Set<Long> excludedImageIds =
        motivationImageFeedbackRepository.findByUserId(userId).stream()
            .map(feedback -> feedback.getImage().getId())
            .collect(Collectors.toSet());
    ensureFeedCards(goal, normalizedLimit + excludedImageIds.size());
    List<MotivationImage> selectedImages =
        selectFeedImageEntities(goal, excludedImageIds, normalizedLimit, state);
    if (selectedImages.size() < normalizedLimit) {
      ensureFeedCards(goal, normalizedLimit + excludedImageIds.size() + 6);
      selectedImages = selectFeedImageEntities(goal, excludedImageIds, normalizedLimit, state);
    }
    List<Long> selectedIds = selectedImages.stream().map(MotivationImage::getId).toList();
    FeedRefreshState updatedState = state.withPreviousImageIds(Set.copyOf(selectedIds));
    feedRefreshStates.put(feedKey(userId, goal.getId()), updatedState);
    boolean reusedPrevious =
        state.feedVersion() > 0
            && !state.previousImageIds().isEmpty()
            && selectedIds.stream().anyMatch(state.previousImageIds()::contains);
    List<MotivationDtos.MotivationImageResponse> images =
        toFeedImageResponses(goal, selectedImages, updatedState.feedVersion());

    GoalExperienceDtos.TodayStatusResponse todayStatus = null;
    try {
      todayStatus = goalCommitmentService.getTodayStatus(userId, goal.getId());
    } catch (Exception ignored) {
      // Goal может еще не иметь commitment; feed все равно должен работать.
    }

    return new MotivationDtos.MotivationFeedResponse(
        images,
        getOrCreateDailyQuote(goal, LocalDate.now()),
        todayStatus != null
            ? todayStatus.nextRecommendedAction()
            : "Сделайте хотя бы одну фокус-сессию и подготовьте доказательство результата по цели.",
        updatedState.refreshSessionId(),
        updatedState.feedVersion(),
        updatedState.generatedAt(),
        reusedPrevious
            ? "Новые изображения временно не найдены, мы обновили порядок и цитаты."
            : null);
  }

  public MotivationDtos.FeedbackResponse markImageNotInteresting(Long userId, Long imageId) {
    MotivationImage image =
        motivationImageRepository
            .findById(imageId)
            .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Image not found"));
    if (motivationImageFeedbackRepository.existsByUserIdAndImageIdAndType(
        userId, imageId, MotivationImageFeedbackType.NOT_INTERESTED)) {
      return new MotivationDtos.FeedbackResponse(
          imageId, "OK", "Больше не будем показывать эту карточку");
    }

    motivationImageFeedbackRepository.save(
        MotivationImageFeedback.builder()
            .user(loadUser(userId))
            .image(image)
            .type(MotivationImageFeedbackType.NOT_INTERESTED)
            .createdAt(OffsetDateTime.now())
            .build());
    return new MotivationDtos.FeedbackResponse(
        imageId, "OK", "Больше не будем показывать эту карточку");
  }

  public MotivationDtos.FeedbackResponse reportImage(
      Long userId, Long imageId, MotivationImageReportReason reason, String comment) {
    MotivationImage image =
        motivationImageRepository
            .findById(imageId)
            .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Image not found"));
    if (!motivationImageFeedbackRepository.existsByUserIdAndImageIdAndType(
        userId, imageId, MotivationImageFeedbackType.REPORTED)) {
      motivationImageFeedbackRepository.save(
          MotivationImageFeedback.builder()
              .user(loadUser(userId))
              .image(image)
              .type(MotivationImageFeedbackType.REPORTED)
              .reason(reason)
              .comment(comment)
              .createdAt(OffsetDateTime.now())
              .build());
    }

    long reportCount =
        motivationImageFeedbackRepository.countByImageIdAndType(
            imageId, MotivationImageFeedbackType.REPORTED);
    image.setReportCount((int) reportCount);
    if (reportCount >= GLOBAL_REPORT_THRESHOLD) {
      image.setHiddenGlobally(true);
    }
    motivationImageRepository.save(image);
    return new MotivationDtos.FeedbackResponse(
        imageId, "OK", "Жалоба отправлена. Мы больше не будем показывать эту карточку.");
  }

  private User loadUser(Long userId) {
    return userRepository
        .findById(userId)
        .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found"));
  }

  private Goal resolveGoalForFeed(Long userId, Long goalId) {
    if (goalId != null) {
      return goalService.ownedGoal(userId, goalId);
    }
    return goalService.getGoals(userId).stream()
        .findFirst()
        .map(goalResponse -> goalService.ownedGoal(userId, goalResponse.id()))
        .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Goal not found"));
  }

  private int normalizeLimit(Integer limit) {
    if (limit == null) {
      return DEFAULT_FEED_LIMIT;
    }
    return Math.max(1, Math.min(limit, 20));
  }

  private List<MotivationImage> selectFeedImageEntities(
      Goal goal, Set<Long> excludedImageIds, int limit, FeedRefreshState state) {
    List<MotivationImage> pool =
        motivationImageRepository
            .findTop50ByThemeAndHiddenGloballyFalseOrderByCreatedAtDesc(detectTheme(goal).name())
            .stream()
            .filter(image -> !excludedImageIds.contains(image.getId()))
            .filter(image -> image.getReportCount() < GLOBAL_REPORT_THRESHOLD)
            .filter(image -> isDisplayableImageUrl(image.getImagePath()))
            .toList();

    List<MotivationImage> ordered = new ArrayList<>(pool);
    if (state.feedVersion() <= 0) {
      return ordered.stream()
          .sorted(Comparator.comparing(MotivationImage::getCreatedAt).reversed())
          .limit(limit)
          .toList();
    }

    Collections.shuffle(ordered, new Random(state.seed()));
    if (state.previousImageIds().isEmpty()) {
      return ordered.stream().limit(limit).toList();
    }

    List<MotivationImage> fresh =
        ordered.stream()
            .filter(image -> !state.previousImageIds().contains(image.getId()))
            .collect(Collectors.toCollection(ArrayList::new));
    if (fresh.size() >= limit) {
      return fresh.stream().limit(limit).toList();
    }
    LinkedHashSet<MotivationImage> combined = new LinkedHashSet<>(fresh);
    combined.addAll(ordered);
    return combined.stream().limit(limit).toList();
  }

  private List<MotivationDtos.MotivationImageResponse> toFeedImageResponses(
      Goal goal, List<MotivationImage> images, int feedVersion) {
    List<MotivationDtos.MotivationImageResponse> responses = new ArrayList<>();
    for (int i = 0; i < images.size(); i++) {
      responses.add(toFeedImageResponse(goal, images.get(i), i, feedVersion));
    }
    return responses;
  }

  private String feedKey(Long userId, Long goalId) {
    return userId + ":" + goalId;
  }

  private FeedRefreshState currentFeedRefreshState(Long userId, Long goalId) {
    return feedRefreshStates.computeIfAbsent(
        feedKey(userId, goalId),
        ignored ->
            new FeedRefreshState(
                UUID.randomUUID().toString(),
                0,
                OffsetDateTime.now(),
                ThreadLocalRandom.current().nextLong(),
                Set.of()));
  }

  private FeedRefreshState nextFeedRefreshState(Long userId, Long goalId) {
    FeedRefreshState current = currentFeedRefreshState(userId, goalId);
    FeedRefreshState next =
        new FeedRefreshState(
            UUID.randomUUID().toString(),
            current.feedVersion() + 1,
            OffsetDateTime.now(),
            ThreadLocalRandom.current().nextLong(),
            current.previousImageIds());
    feedRefreshStates.put(feedKey(userId, goalId), next);
    return next;
  }

  private void ensureFeedCards(Goal goal, int limit) {
    String theme = detectTheme(goal).name();
    List<MotivationImage> existingImages =
        motivationImageRepository.findTop50ByThemeAndHiddenGloballyFalseOrderByCreatedAtDesc(theme);
    int validExistingCount =
        (int)
            existingImages.stream()
                .filter(image -> isDisplayableImageUrl(image.getImagePath()))
                .count();

    Set<String> existingSourceUrls =
        existingImages.stream()
            .map(MotivationImage::getSourceUrl)
            .collect(Collectors.toCollection(HashSet::new));
    for (ImageCandidate candidate : searchImageCandidates(goal, limit * 3)) {
      if (existingSourceUrls.contains(candidate.sourceUrl())) {
        continue;
      }
      MotivationImage image =
          motivationImageRepository
              .findTopBySourceUrlOrderByCreatedAtDesc(candidate.sourceUrl())
              .orElseGet(
                  () ->
                      motivationImageRepository.save(
                          MotivationImage.builder()
                              .goal(goal)
                              .imagePath(candidate.imageUrl())
                              .sourceUrl(candidate.sourceUrl())
                              .title(candidate.title())
                              .description(candidate.description())
                              .theme(theme)
                              .prompt(candidate.query())
                              .isFavorite(false)
                              .generatedBy(MotivationImageSource.AUTO)
                              .hiddenGlobally(false)
                              .reportCount(0)
                              .createdAt(OffsetDateTime.now())
                              .build()));
      existingSourceUrls.add(image.getSourceUrl());
      validExistingCount++;
      if (validExistingCount >= limit) {
        return;
      }
    }

    List<FallbackCardTemplate> templates = fallbackTemplates(goal);
    int variant = 0;
    while (validExistingCount < limit && variant < Math.max(limit * 3, templates.size())) {
      FallbackCardTemplate template = templates.get(variant % templates.size());
      String sourceUrl =
          "fallback-photo://"
              + detectTheme(goal).name().toLowerCase(Locale.ROOT)
              + "/"
              + template.slug()
              + "-"
              + variant;
      variant++;
      if (!existingSourceUrls.add(sourceUrl)) {
        continue;
      }

      String imagePath = fallbackPhotoUrl(goal, variant);
      motivationImageRepository.save(
          MotivationImage.builder()
              .goal(goal)
              .imagePath(imagePath)
              .sourceUrl(sourceUrl)
              .title(template.title())
              .description(template.description())
              .theme(theme)
              .prompt(template.caption())
              .isFavorite(false)
              .generatedBy(MotivationImageSource.AUTO)
              .hiddenGlobally(false)
              .reportCount(0)
              .createdAt(OffsetDateTime.now())
              .build());
      validExistingCount++;
    }
  }

  private List<ImageCandidate> searchImageCandidates(Goal goal, int limit) {
    List<ImageCandidate> candidates = new ArrayList<>();
    Set<String> uniqueSources = new LinkedHashSet<>();
    for (String query : buildSearchQueries(goal)) {
      if (candidates.size() >= limit) {
        break;
      }
      try {
        String response =
            webImageWebClient
                .get()
                .uri(
                    uriBuilder ->
                        uriBuilder
                            .path("/w/api.php")
                            .queryParam("action", "query")
                            .queryParam("format", "json")
                            .queryParam("generator", "search")
                            .queryParam("gsrnamespace", 6)
                            .queryParam("gsrlimit", 20)
                            .queryParam("gsrsearch", query)
                            .queryParam("prop", "imageinfo")
                            .queryParam("iiprop", "url")
                            .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        if (response == null || response.isBlank()) {
          continue;
        }
        JsonNode pages = OBJECT_MAPPER.readTree(response).path("query").path("pages");
        if (!pages.isObject()) {
          continue;
        }
        pages
            .elements()
            .forEachRemaining(
                page -> {
                  String imageUrl =
                      Optional.ofNullable(
                              page.path("imageinfo").isArray()
                                  ? page.path("imageinfo").get(0)
                                  : null)
                          .map(node -> node.path("url").asText())
                          .orElse("");
                  String sourceUrl =
                      Optional.ofNullable(
                              page.path("imageinfo").isArray()
                                  ? page.path("imageinfo").get(0)
                                  : null)
                          .map(node -> node.path("descriptionurl").asText(imageUrl))
                          .orElse(imageUrl);
                  if (imageUrl.isBlank()
                      || sourceUrl.isBlank()
                      || !isDisplayableImageUrl(imageUrl)
                      || !uniqueSources.add(sourceUrl)) {
                    return;
                  }
                  String title =
                      sanitizeFeedTitle(
                          formatCommonsTitle(page.path("title").asText("Motivation image")), goal);
                  String description = sanitizeFeedDescription(page.path("title").asText(""), goal);
                  candidates.add(
                      new ImageCandidate(imageUrl, sourceUrl, title, description, query));
                });
      } catch (Exception e) {
        log.warn(
            "Не удалось получить интернет-изображения по запросу '{}': {}", query, e.getMessage());
      }
    }
    return candidates;
  }

  private List<String> buildSearchQueries(Goal goal) {
    GoalTheme theme = detectTheme(goal);
    List<String> themeTerms =
        switch (theme) {
          case SPORT -> SPORT_TERMS;
          case STUDY -> STUDY_TERMS;
          case CODE -> CODE_TERMS;
          default -> GENERAL_TERMS;
        };
    List<String> keywords =
        extractKeywords(
            goal.getTitle() + " " + (goal.getDescription() == null ? "" : goal.getDescription()));
    List<String> queries = new ArrayList<>();
    if (!keywords.isEmpty()) {
      queries.add(String.join(" ", keywords));
      queries.add(String.join(" ", keywords) + " motivation");
    }
    queries.addAll(themeTerms);
    if (queries.isEmpty()) {
      queries.addAll(GENERAL_TERMS);
    }
    return queries;
  }

  private List<String> extractKeywords(String text) {
    String normalized =
        text.toLowerCase(Locale.ROOT)
            .replaceAll("[^\\p{L}\\p{Nd}\\s]", " ")
            .replaceAll("\\s+", " ")
            .trim();
    if (normalized.isBlank()) {
      return List.of();
    }
    LinkedHashSet<String> result = new LinkedHashSet<>();
    for (String token : normalized.split(" ")) {
      if (token.length() < 4) {
        continue;
      }
      result.add(token);
      if (result.size() >= 5) {
        break;
      }
    }
    return List.copyOf(result);
  }

  private String formatCommonsTitle(String rawTitle) {
    String title = rawTitle == null ? "Motivation image" : rawTitle;
    title = title.replaceFirst("^File:", "");
    title = title.replace('_', ' ');
    title = title.replaceAll("\\.[A-Za-z0-9]{2,5}$", "");
    return title.length() > 255 ? title.substring(0, 255) : title;
  }

  private MotivationDtos.MotivationResponse saveImage(
      Goal goal, String styleOptions, MotivationImageSource source) {
    var image =
        aiService.generateMotivationImage(
            new com.pomodoro.app.dto.AiDtos.GoalContext(
                goal.getId(), goal.getTitle(), goal.getDescription()),
            styleOptions);
    MotivationImage saved =
        motivationImageRepository.save(
            MotivationImage.builder()
                .goal(goal)
                .imagePath(image.imagePath())
                .sourceUrl(image.imagePath())
                .title(goal.getTitle())
                .description(goal.getDescription())
                .theme(detectTheme(goal).name())
                .prompt(image.usedPrompt())
                .isFavorite(false)
                .generatedBy(source)
                .hiddenGlobally(false)
                .reportCount(0)
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
    if (image.getImagePath().startsWith("/uploads/")) {
      storageService.deletePublicPath(image.getImagePath());
    }
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

  private MotivationDtos.MotivationImageResponse toFeedImageResponse(
      Goal goal, MotivationImage image, int cardIndex, int feedVersion) {
    return new MotivationDtos.MotivationImageResponse(
        image.getId(),
        image.getImagePath(),
        sanitizeFeedTitle(image.getTitle(), goal),
        sanitizeFeedDescription(image.getDescription(), goal),
        buildCaption(goal, image),
        buildDisplayQuote(goal, cardIndex, feedVersion),
        buildGoalReason(goal, image),
        image.getCreatedAt());
  }

  private String buildDisplayQuote(Goal goal, int cardIndex, int feedVersion) {
    List<String> pool = displayQuotePool(goal);
    if (pool.isEmpty()) {
      return "Сделай ещё один маленький шаг сегодня — именно так цель становится реальностью.";
    }
    int index = Math.floorMod(feedVersion * 3 + cardIndex, pool.size());
    return pool.get(index);
  }

  private List<String> displayQuotePool(Goal goal) {
    LinkedHashSet<String> quotes = new LinkedHashSet<>();
    pickQuotePool(goal).stream().map(QuoteItem::textRu).forEach(quotes::add);
    fallbackTemplates(goal).stream().map(FallbackCardTemplate::caption).forEach(quotes::add);
    quotes.add("Не жди идеального настроения: начни с одного короткого действия.");
    quotes.add("Маленький честный шаг сегодня сильнее большого обещания на завтра.");
    quotes.add("Фокус на ближайшие 25 минут важнее тревоги обо всём пути.");
    quotes.add("Дисциплина — это возвращение к цели после каждой паузы.");
    quotes.add("Результат растёт там, где действия повторяются достаточно долго.");
    quotes.add("Сделай видимый прогресс и зафиксируй его отчётом.");
    quotes.add("Одна завершённая сессия лучше десяти отложенных планов.");
    quotes.add("Не нужно делать идеально — нужно продолжать достаточно регулярно.");
    quotes.add("Сегодняшний темп держится на одном следующем действии.");
    quotes.add("Когда цель большая, уменьши шаг, но не останавливай движение.");
    return List.copyOf(quotes);
  }

  private boolean isDisplayableImageUrl(String imageUrl) {
    if (imageUrl == null || imageUrl.isBlank()) {
      return false;
    }
    String normalized = imageUrl.toLowerCase(Locale.ROOT);
    if (normalized.startsWith("/uploads/")) {
      return !normalized.endsWith(".svg");
    }
    if (!(normalized.startsWith("http://") || normalized.startsWith("https://"))) {
      return false;
    }
    if (normalized.contains(".pdf")
        || normalized.contains(".html")
        || normalized.contains(".htm")
        || normalized.contains("wikimedia.org/wiki/")
        || normalized.contains("commons.wikimedia.org/wiki/")) {
      return false;
    }
    if (normalized.contains("images.unsplash.com/")) {
      return true;
    }
    return normalized.matches(".*\\.(jpg|jpeg|png|webp|gif)(\\?.*)?$");
  }

  private String sanitizeFeedTitle(String rawTitle, Goal goal) {
    String title = rawTitle == null ? "" : rawTitle.trim();
    if (title.isBlank() || looksTechnicalText(title) || isGoalEcho(title, goal)) {
      if (goal.getTitle() == null || goal.getTitle().isBlank()) {
        return "Визуальная мотивация";
      }
      return "Мотивация для цели";
    }
    if (title.length() > 80) {
      title = title.substring(0, 80).trim();
    }
    return title;
  }

  private String sanitizeFeedDescription(String rawDescription, Goal goal) {
    String text = rawDescription == null ? "" : rawDescription.trim();
    if (text.isBlank() || looksTechnicalText(text) || isGoalEcho(text, goal)) {
      return "Короткий фокус сегодня превращает намерение в реальный прогресс.";
    }
    if (text.length() > 180) {
      text = text.substring(0, 180).trim() + "…";
    }
    return text;
  }

  private boolean isGoalEcho(String value, Goal goal) {
    String text = normalizeFeedText(value);
    String title = normalizeFeedText(goal.getTitle());
    String description = normalizeFeedText(goal.getDescription());
    return !text.isBlank()
        && ((title.length() > 3 && (text.equals(title) || text.contains(title)))
            || (description.length() > 8
                && (text.equals(description)
                    || text.contains(description)
                    || description.contains(text))));
  }

  private String normalizeFeedText(String value) {
    return value == null
        ? ""
        : value.toLowerCase(Locale.ROOT)
            .replace('ё', 'е')
            .replaceAll("[^\\p{L}\\p{Nd}\\s]", " ")
            .replaceAll("\\s+", " ")
            .trim();
  }

  private boolean looksTechnicalText(String value) {
    String normalized = value.toLowerCase(Locale.ROOT).trim();
    if (normalized.isBlank()) {
      return true;
    }
    return normalized.startsWith("file:")
        || normalized.startsWith("http://")
        || normalized.startsWith("https://")
        || normalized.contains(".pdf")
        || normalized.contains(".svg")
        || normalized.contains(".jpg")
        || normalized.contains(".jpeg")
        || normalized.contains(".png")
        || normalized.contains("wikimedia")
        || normalized.contains("commons")
        || normalized.matches(".*[\\\\/_].*")
        || normalized.matches("^[a-z0-9\\-_.]{18,}$");
  }

  private String buildCaption(Goal goal, MotivationImage image) {
    String description = image.getDescription();
    if (description != null
        && !looksTechnicalText(description)
        && !isGoalEcho(description, goal)
        && !description.isBlank()) {
      return description;
    }
    return "Каждая Pomodoro-сессия делает следующий шаг проще и заметнее.";
  }

  private String buildGoalReason(Goal goal, MotivationImage image) {
    String theme = image.getTheme() == null ? "GENERAL" : image.getTheme();
    return switch (theme) {
      case "SPORT" -> "Подобрано под спортивную цель и визуально поддерживает ритм тренировок.";
      case "CODE" ->
          "Подобрано под техническую цель: помогает удерживать внимание на практике и результате.";
      case "STUDY" -> "Подобрано под учебную цель: поддерживает фокус на регулярном обучении.";
      default -> "Подобрано по активной цели и её описанию, чтобы поддержать сегодняшний темп.";
    };
  }

  private List<FallbackCardTemplate> fallbackTemplates(Goal goal) {
    return switch (detectTheme(goal)) {
      case SPORT -> SPORT_FALLBACK_CARDS;
      case CODE -> CODE_FALLBACK_CARDS;
      case STUDY -> STUDY_FALLBACK_CARDS;
      default -> GENERAL_FALLBACK_CARDS;
    };
  }

  private String fallbackPhotoUrl(Goal goal, int variant) {
    List<String> photos =
        switch (detectTheme(goal)) {
          case SPORT -> SPORT_FALLBACK_PHOTOS;
          case CODE -> CODE_FALLBACK_PHOTOS;
          case STUDY -> STUDY_FALLBACK_PHOTOS;
          default -> GENERAL_FALLBACK_PHOTOS;
        };
    return photos.get(Math.floorMod(variant, photos.size()));
  }

  private String createFallbackImage(Goal goal, FallbackCardTemplate template) {
    String background =
        goal.getThemeColor() == null || goal.getThemeColor().isBlank()
            ? "#dff6e5"
            : goal.getThemeColor();
    String accent = darkenColor(background, 0.58);
    String svg =
        """
        <svg xmlns='http://www.w3.org/2000/svg' width='900' height='1400' viewBox='0 0 900 1400'>
          <defs>
            <linearGradient id='bg' x1='0' y1='0' x2='1' y2='1'>
              <stop offset='0%%' stop-color='%s' />
              <stop offset='100%%' stop-color='#f6ecdf' />
            </linearGradient>
          </defs>
          <rect width='100%%' height='100%%' rx='48' fill='url(#bg)' />
          <circle cx='720' cy='250' r='170' fill='#ffffff44' />
          <circle cx='180' cy='1160' r='190' fill='#ffffff2f' />
          <text x='72' y='180' fill='%s' font-size='42' font-family='Arial, sans-serif' font-weight='700'>Pomodoro Web</text>
          <text x='72' y='300' fill='%s' font-size='58' font-family='Arial, sans-serif' font-weight='700'>%s</text>
          <foreignObject x='72' y='380' width='756' height='560'>
            <div xmlns='http://www.w3.org/1999/xhtml' style='font-family:Arial,sans-serif;color:%s;font-size:30px;line-height:1.45;'>
              %s
            </div>
          </foreignObject>
          <foreignObject x='72' y='1010' width='756' height='220'>
            <div xmlns='http://www.w3.org/1999/xhtml' style='font-family:Arial,sans-serif;color:%s;font-size:24px;line-height:1.45;'>
              %s
            </div>
          </foreignObject>
        </svg>
        """
            .formatted(
                escapeXml(background),
                escapeXml(accent),
                escapeXml(accent),
                escapeXml(template.title()),
                escapeXml(accent),
                escapeXml(template.description()),
                escapeXml(accent),
                escapeXml(template.caption()));
    return storageService.storeBytes(
        svg.getBytes(java.nio.charset.StandardCharsets.UTF_8), "motivation", "svg");
  }

  private String darkenColor(String hex, double factor) {
    String normalized = hex == null ? "" : hex.trim();
    if (!normalized.matches("^#[0-9A-Fa-f]{6}$")) {
      return "#173b35";
    }
    int value = Integer.parseInt(normalized.substring(1), 16);
    int r = (int) (((value >> 16) & 0xff) * factor);
    int g = (int) (((value >> 8) & 0xff) * factor);
    int b = (int) ((value & 0xff) * factor);
    return "#%02x%02x%02x".formatted(r, g, b);
  }

  private String escapeXml(String value) {
    return value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;");
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
            .toLowerCase(Locale.ROOT);
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

  private record ImageCandidate(
      String imageUrl, String sourceUrl, String title, String description, String query) {}

  private record FeedRefreshState(
      String refreshSessionId,
      int feedVersion,
      OffsetDateTime generatedAt,
      long seed,
      Set<Long> previousImageIds) {
    FeedRefreshState withPreviousImageIds(Set<Long> nextPreviousImageIds) {
      return new FeedRefreshState(
          refreshSessionId, feedVersion, generatedAt, seed, nextPreviousImageIds);
    }
  }

  private record FallbackCardTemplate(String title, String description, String caption) {
    private String slug() {
      return title.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{Nd}]+", "-");
    }
  }
}
