package com.pomodoro.app.service;

import com.pomodoro.app.dto.GoalDtos;
import com.pomodoro.app.dto.GoalExperienceDtos;
import com.pomodoro.app.entity.DailySummary;
import com.pomodoro.app.entity.FocusSession;
import com.pomodoro.app.entity.Goal;
import com.pomodoro.app.entity.GoalCommitment;
import com.pomodoro.app.entity.Report;
import com.pomodoro.app.enums.GoalStatus;
import com.pomodoro.app.enums.CommitmentStatus;
import com.pomodoro.app.enums.GoalEventType;
import com.pomodoro.app.enums.ReportStatus;
import com.pomodoro.app.enums.RiskStatus;
import com.pomodoro.app.exception.AppException;
import com.pomodoro.app.repository.DailySummaryRepository;
import com.pomodoro.app.repository.FocusSessionRepository;
import com.pomodoro.app.repository.GoalCommitmentRepository;
import com.pomodoro.app.repository.GoalRepository;
import com.pomodoro.app.repository.ReportRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GoalCommitmentService {
  private final GoalCommitmentRepository goalCommitmentRepository;
  private final GoalRepository goalRepository;
  private final GoalService goalService;
  private final GoalEventService goalEventService;
  private final FocusSessionRepository focusSessionRepository;
  private final ReportRepository reportRepository;
  private final DailySummaryRepository dailySummaryRepository;

  public GoalCommitmentService(
      GoalCommitmentRepository goalCommitmentRepository,
      GoalRepository goalRepository,
      GoalService goalService,
      GoalEventService goalEventService,
      FocusSessionRepository focusSessionRepository,
      ReportRepository reportRepository,
      DailySummaryRepository dailySummaryRepository) {
    this.goalCommitmentRepository = goalCommitmentRepository;
    this.goalRepository = goalRepository;
    this.goalService = goalService;
    this.goalEventService = goalEventService;
    this.focusSessionRepository = focusSessionRepository;
    this.reportRepository = reportRepository;
    this.dailySummaryRepository = dailySummaryRepository;
  }

  @Transactional
  public GoalExperienceDtos.CommitmentResponse createCommitment(
      Long userId, Long goalId, GoalExperienceDtos.CommitmentCreateRequest request) {
    Goal goal = goalService.ownedGoal(userId, goalId);
    if (request.endDate() != null && request.endDate().isBefore(request.startDate())) {
      throw new AppException(
          HttpStatus.BAD_REQUEST, "Дата окончания обязательства не может быть раньше даты начала.");
    }
    goalCommitmentRepository
        .findByGoalIdAndUserIdAndStatus(goalId, userId, CommitmentStatus.ACTIVE)
        .ifPresent(
            existing -> {
              throw new AppException(
                  HttpStatus.BAD_REQUEST,
                  "Для этой цели уже существует активное ежедневное обязательство.");
            });

    OffsetDateTime now = OffsetDateTime.now();
    GoalCommitment commitment = new GoalCommitment();
    commitment.setUser(goal.getUser());
    commitment.setGoal(goal);
    commitment.setDailyTargetMinutes(request.dailyTargetMinutes());
    commitment.setStartDate(request.startDate());
    commitment.setEndDate(request.endDate());
    commitment.setStatus(CommitmentStatus.ACTIVE);
    commitment.setDisciplineScore(80);
    commitment.setCurrentStreak(0);
    commitment.setBestStreak(0);
    commitment.setCompletedDays(0);
    commitment.setMissedDays(0);
    commitment.setPersonalRewardTitle(blankToNull(request.personalRewardTitle()));
    commitment.setPersonalRewardDescription(blankToNull(request.personalRewardDescription()));
    commitment.setRewardUnlocked(false);
    commitment.setRiskStatus(RiskStatus.LOW);
    commitment.setCreatedAt(now);
    commitment.setUpdatedAt(now);

    GoalCommitment saved = goalCommitmentRepository.save(commitment);
    goalEventService.createEvent(
        goal,
        saved,
        GoalEventType.COMMITMENT_CREATED,
        "Создано ежедневное обязательство",
        "Для цели задана дневная норма %d минут.".formatted(saved.getDailyTargetMinutes()),
        null,
        String.valueOf(saved.getDailyTargetMinutes()));
    return toCommitmentResponse(saved);
  }

  @Transactional(readOnly = true)
  public GoalExperienceDtos.CommitmentResponse getCommitment(Long userId, Long goalId) {
    goalService.ownedGoal(userId, goalId);
    GoalCommitment commitment =
        goalCommitmentRepository
            .findFirstByGoalIdAndUserIdOrderByCreatedAtDesc(goalId, userId)
            .orElseThrow(
                () ->
                    new AppException(
                        HttpStatus.NOT_FOUND,
                        "Для этой цели ещё не настроено ежедневное обязательство."));
    return toCommitmentResponse(commitment);
  }

  @Transactional(readOnly = true)
  public GoalExperienceDtos.TodayStatusResponse getTodayStatus(Long userId, Long goalId) {
    Goal goal = goalService.ownedGoal(userId, goalId);
    GoalCommitment commitment = latestCommitment(goal.getId(), userId).orElse(null);
    return buildTodayStatus(goal, commitment, LocalDate.now(ZoneId.systemDefault()));
  }

  @Transactional(readOnly = true)
  public GoalExperienceDtos.ForecastResponse getForecast(Long userId, Long goalId) {
    Goal goal = goalService.ownedGoal(userId, goalId);
    GoalCommitment commitment = latestCommitment(goal.getId(), userId).orElse(null);
    return buildForecast(goal, commitment);
  }

  @Transactional(readOnly = true)
  public GoalExperienceDtos.GoalExperienceResponse getGoalExperience(Long userId, Long goalId) {
    Goal goal = goalService.ownedGoal(userId, goalId);
    GoalCommitment commitment = latestCommitment(goal.getId(), userId).orElse(null);
    GoalExperienceDtos.TodayStatusResponse today =
        buildTodayStatus(goal, commitment, LocalDate.now(ZoneId.systemDefault()));
    GoalExperienceDtos.ForecastResponse forecast = buildForecast(goal, commitment);
    String recommendation = buildAiRecommendation(commitment, today, forecast);
    return new GoalExperienceDtos.GoalExperienceResponse(
        toGoalResponse(goal),
        commitment != null ? toCommitmentResponse(commitment) : null,
        today,
        forecast,
        goalEventService.getRecentEvents(goalId, userId),
        recommendation);
  }

  @Transactional(readOnly = true)
  public List<GoalExperienceDtos.GoalExperienceResponse> getDashboardExperience(Long userId) {
    return goalRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, GoalStatus.ACTIVE).stream()
        .map(goal -> getGoalExperience(userId, goal.getId()))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<GoalExperienceDtos.GoalEventResponse> getEvents(Long userId, Long goalId) {
    goalService.ownedGoal(userId, goalId);
    return goalEventService.getRecentEvents(goalId, userId);
  }

  @Transactional
  public void onFocusSessionStarted(Goal goal) {
    GoalCommitment commitment = latestCommitment(goal.getId(), goal.getUser().getId()).orElse(null);
    goalEventService.createEvent(
        goal,
        commitment,
        GoalEventType.FOCUS_SESSION_STARTED,
        "Фокус-сессия начата",
        "Пользователь начал новую Pomodoro-сессию по цели.",
        null,
        null);
  }

  @Transactional
  public void onFocusSessionCompleted(Goal goal, Integer durationMinutes) {
    GoalCommitment commitment = latestCommitment(goal.getId(), goal.getUser().getId()).orElse(null);
    goalEventService.createEvent(
        goal,
        commitment,
        GoalEventType.FOCUS_SESSION_COMPLETED,
        "Фокус-сессия завершена",
        "Pomodoro-сессия завершена за %d минут."
            .formatted(durationMinutes == null ? 0 : durationMinutes),
        null,
        String.valueOf(durationMinutes == null ? 0 : durationMinutes));
  }

  @Transactional
  public void onReportSubmitted(Report report) {
    Goal goal = report.getGoal();
    GoalCommitment commitment = latestCommitment(goal.getId(), goal.getUser().getId()).orElse(null);
    goalEventService.createEvent(
        goal,
        commitment,
        GoalEventType.REPORT_SUBMITTED,
        "Фото-отчёт отправлен",
        "Отчёт отправлен на AI-проверку за %s.".formatted(report.getReportDate()),
        null,
        report.getStatus().name());

    GoalEventType resultType =
        switch (report.getStatus()) {
          case CONFIRMED -> GoalEventType.REPORT_APPROVED;
          case REJECTED -> GoalEventType.REPORT_REJECTED;
          case PENDING -> GoalEventType.REPORT_NEEDS_MORE_INFO;
          case OVERDUE -> null;
        };

    if (resultType != null) {
      String title =
          switch (resultType) {
            case REPORT_APPROVED -> "Отчёт подтверждён";
            case REPORT_REJECTED -> "Отчёт отклонён";
            case REPORT_NEEDS_MORE_INFO -> "Нужно больше данных по отчёту";
            default -> "";
          };
      goalEventService.createEvent(
          goal,
          commitment,
          resultType,
          title,
          report.getAiExplanation(),
          null,
          report.getAiVerdict() != null ? report.getAiVerdict().name() : null);
    }
  }

  @Transactional
  public void processPreviousDay(LocalDate date) {
    OffsetDateTime eventTimestamp =
        date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
    for (GoalCommitment commitment :
        goalCommitmentRepository.findByStatus(CommitmentStatus.ACTIVE)) {
      if (date.isBefore(commitment.getStartDate())) {
        continue;
      }
      if (goalEventService.hasProcessedDay(commitment.getId(), date.toString())) {
        continue;
      }
      Goal goal = commitment.getGoal();
      int focusMinutes = focusMinutesForDate(goal.getId(), date);
      ReportStatus reportStatus = resolveReportStatus(goal.getId(), date);
      boolean confirmedReport = reportStatus == ReportStatus.CONFIRMED;
      boolean completedDay = focusMinutes >= commitment.getDailyTargetMinutes() && confirmedReport;

      int previousStreak = commitment.getCurrentStreak();
      int previousScore = commitment.getDisciplineScore();
      RiskStatus previousRisk = commitment.getRiskStatus();

      if (completedDay) {
        commitment.setCompletedDays(commitment.getCompletedDays() + 1);
        commitment.setCurrentStreak(commitment.getCurrentStreak() + 1);
        commitment.setBestStreak(
            Math.max(commitment.getBestStreak(), commitment.getCurrentStreak()));
        commitment.setDisciplineScore(Math.min(100, commitment.getDisciplineScore() + 3));
        goalEventService.createEvent(
            goal,
            commitment,
            GoalEventType.DAY_COMPLETED,
            "День засчитан",
            "За %s выполнена дневная норма и подтверждён фото-отчёт.".formatted(date),
            null,
            date.toString(),
            eventTimestamp);
      } else {
        commitment.setMissedDays(commitment.getMissedDays() + 1);
        commitment.setCurrentStreak(0);
        commitment.setDisciplineScore(Math.max(0, commitment.getDisciplineScore() - 10));
        goalEventService.createEvent(
            goal,
            commitment,
            GoalEventType.DAY_MISSED,
            "День пропущен",
            "За %s день не был засчитан: %s."
                .formatted(date, missedDayReason(focusMinutes, commitment, reportStatus)),
            null,
            date.toString(),
            eventTimestamp);
      }

      RiskStatus recalculatedRisk =
          calculateRiskStatus(commitment.getDisciplineScore(), commitment.getMissedDays());
      commitment.setRiskStatus(recalculatedRisk);
      commitment.setUpdatedAt(OffsetDateTime.now());
      goal.setCurrentStreak(commitment.getCurrentStreak());

      if (previousStreak != commitment.getCurrentStreak()) {
        goalEventService.createEvent(
            goal,
            commitment,
            GoalEventType.STREAK_UPDATED,
            "Серия обновлена",
            "Текущая серия изменена после закрытия дня.",
            String.valueOf(previousStreak),
            String.valueOf(commitment.getCurrentStreak()),
            eventTimestamp);
      }

      if (previousScore != commitment.getDisciplineScore()) {
        goalEventService.createEvent(
            goal,
            commitment,
            GoalEventType.DISCIPLINE_SCORE_CHANGED,
            "Изменился показатель дисциплины",
            "После закрытия дня пересчитан показатель дисциплины.",
            String.valueOf(previousScore),
            String.valueOf(commitment.getDisciplineScore()),
            eventTimestamp);
      }

      if (previousRisk != recalculatedRisk) {
        goalEventService.createEvent(
            goal,
            commitment,
            GoalEventType.RISK_STATUS_CHANGED,
            "Изменился статус риска",
            "Статус риска обновлён на основе последних дней выполнения.",
            previousRisk.name(),
            recalculatedRisk.name(),
            eventTimestamp);
      }

      if (commitment.getEndDate() != null && !date.isBefore(commitment.getEndDate())) {
        int plannedDays = plannedDays(commitment);
        int requiredDays = Math.max(1, (int) Math.ceil(plannedDays * 0.7d));
        if (commitment.getCompletedDays() >= requiredDays
            && commitment.getRiskStatus() != RiskStatus.HIGH) {
          if (!Boolean.TRUE.equals(commitment.getRewardUnlocked())) {
            commitment.setRewardUnlocked(true);
            goalEventService.createEvent(
                goal,
                commitment,
                GoalEventType.REWARD_UNLOCKED,
                "Награда разблокирована",
                rewardDescription(commitment),
                null,
                commitment.getPersonalRewardTitle(),
                eventTimestamp);
          }
          commitment.setStatus(CommitmentStatus.COMPLETED);
          goalService.markCompleted(goal, rewardDescription(commitment));
        } else {
          commitment.setStatus(CommitmentStatus.FAILED);
          goalService.markFailed(
              goal,
              "Обязательство завершилось без нужного количества подтверждённых дней или с высоким риском.");
        }
      }

      goalRepository.save(goal);
      goalCommitmentRepository.save(commitment);
    }
  }

  public RiskStatus calculateRiskStatus(int disciplineScore, int missedDays) {
    if (disciplineScore < 50 || missedDays >= 4) {
      return RiskStatus.HIGH;
    }
    if ((disciplineScore >= 50 && disciplineScore <= 74) || (missedDays >= 2 && missedDays <= 3)) {
      return RiskStatus.MEDIUM;
    }
    return RiskStatus.LOW;
  }

  private GoalExperienceDtos.TodayStatusResponse buildTodayStatus(
      Goal goal, GoalCommitment commitment, LocalDate date) {
    int focusMinutes = focusMinutesForDate(goal.getId(), date);
    ReportStatus reportStatus = resolveReportStatus(goal.getId(), date);
    boolean approved = reportStatus == ReportStatus.CONFIRMED;
    Integer dailyTargetMinutes = commitment != null ? commitment.getDailyTargetMinutes() : null;
    Integer remaining =
        dailyTargetMinutes == null ? null : Math.max(dailyTargetMinutes - focusMinutes, 0);
    boolean targetReached = dailyTargetMinutes != null && focusMinutes >= dailyTargetMinutes;
    boolean completed = targetReached && approved;

    String message;
    String action;
    if (commitment == null) {
      message =
          "У цели ещё нет ежедневного обязательства. Настройте его, чтобы система считала streak и риск.";
      action = "Настройте дневную норму и личную награду для этой цели.";
    } else if (Boolean.TRUE.equals(commitment.getRewardUnlocked())) {
      message =
          "Награда уже разблокирована. Продолжайте поддерживать ритм, чтобы не потерять форму.";
      action = "Поддержите результат короткой Pomodoro-сессией и новым отчётом.";
    } else if (completed) {
      message = "Сегодняшний день уже засчитан. Вы сохранили серию и укрепили дисциплину.";
      action = "Можно завершить день или сделать дополнительную сессию в запас.";
    } else if (!targetReached) {
      message = "До дневной нормы ещё есть путь. Один короткий фокус-блок уже приблизит цель.";
      action =
          "Осталось %d минут до дневной нормы. Сделайте следующую Pomodoro-сессию."
              .formatted(Math.max(remaining == null ? 0 : remaining, 0));
    } else if (!approved) {
      message = "Норма по времени закрыта, но день ещё не подтверждён отчётом.";
      action = "Отправьте фото-отчёт, чтобы день был засчитан и серия сохранилась.";
    } else {
      message = "Сегодня уже есть прогресс по цели.";
      action = "Продолжайте держать выбранный ритм.";
    }

    return new GoalExperienceDtos.TodayStatusResponse(
        goal.getId(),
        goal.getTitle(),
        dailyTargetMinutes,
        focusMinutes,
        remaining,
        reportStatus,
        approved,
        targetReached,
        completed,
        commitment != null ? commitment.getDisciplineScore() : null,
        commitment != null ? commitment.getCurrentStreak() : goal.getCurrentStreak(),
        commitment != null ? commitment.getRiskStatus() : null,
        message,
        action);
  }

  private GoalExperienceDtos.ForecastResponse buildForecast(Goal goal, GoalCommitment commitment) {
    List<FocusSession> sessions =
        focusSessionRepository.findByGoalIdOrderByStartedAtDesc(goal.getId()).stream()
            .filter(session -> session.getDurationMinutes() != null)
            .toList();
    int totalFocusMinutes = sessions.stream().mapToInt(FocusSession::getDurationMinutes).sum();
    List<DailySummary> summaries =
        dailySummaryRepository.findByGoalIdOrderBySummaryDateAsc(goal.getId());
    double averageDailyMinutes =
        summaries.isEmpty()
            ? sessions.stream()
                .collect(
                    java.util.stream.Collectors.groupingBy(
                        session -> session.getStartedAt().toLocalDate(),
                        java.util.stream.Collectors.summingInt(FocusSession::getDurationMinutes)))
                .values()
                .stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0)
            : summaries.stream().mapToInt(DailySummary::getFocusMinutes).average().orElse(0);

    if (goal.getTargetHours() == null) {
      return new GoalExperienceDtos.ForecastResponse(
          goal.getId(),
          null,
          totalFocusMinutes,
          roundAverage(averageDailyMinutes),
          null,
          null,
          false,
          commitment != null && commitment.getRiskStatus() == RiskStatus.HIGH ? "LOW" : "MEDIUM",
          "Для точного прогноза укажите targetHours у цели. Пока система опирается только на текущий темп фокуса.");
    }

    int targetTotalMinutes = goal.getTargetHours().multiply(BigDecimal.valueOf(60)).intValue();
    int remainingMinutes = Math.max(targetTotalMinutes - totalFocusMinutes, 0);
    LocalDate estimatedCompletionDate = null;
    if (remainingMinutes == 0) {
      estimatedCompletionDate = LocalDate.now(ZoneId.systemDefault());
    } else if (averageDailyMinutes > 0) {
      int remainingDays = (int) Math.ceil(remainingMinutes / averageDailyMinutes);
      estimatedCompletionDate = LocalDate.now(ZoneId.systemDefault()).plusDays(remainingDays);
    }

    boolean onTrack =
        goal.getDeadline() == null
            || estimatedCompletionDate == null
            || !estimatedCompletionDate.isAfter(goal.getDeadline());
    String probabilityLabel;
    if (onTrack && commitment != null && commitment.getDisciplineScore() >= 75) {
      probabilityLabel = "HIGH";
    } else if (commitment != null && commitment.getDisciplineScore() >= 50) {
      probabilityLabel = "MEDIUM";
    } else {
      probabilityLabel = "LOW";
    }

    String explanation;
    if (estimatedCompletionDate == null) {
      explanation =
          "Недостаточно накопленных фокус-сессий, чтобы оценить дату завершения. Сделайте несколько сессий подряд.";
    } else if (goal.getDeadline() != null) {
      explanation =
          onTrack
              ? "При текущем темпе цель укладывается в дедлайн."
              : "Текущий темп ниже нужного: цель может выйти за дедлайн.";
    } else {
      explanation = "Прогноз рассчитан по текущему среднему объёму фокус-работы без дедлайна.";
    }

    return new GoalExperienceDtos.ForecastResponse(
        goal.getId(),
        goal.getTargetHours(),
        totalFocusMinutes,
        roundAverage(averageDailyMinutes),
        remainingMinutes,
        estimatedCompletionDate,
        onTrack,
        probabilityLabel,
        explanation);
  }

  private String buildAiRecommendation(
      GoalCommitment commitment,
      GoalExperienceDtos.TodayStatusResponse today,
      GoalExperienceDtos.ForecastResponse forecast) {
    if (commitment == null) {
      return "Начните с настройки ежедневного обязательства: так система сможет считать дисциплину, риск и прогноз достижения цели.";
    }
    if (Boolean.TRUE.equals(commitment.getRewardUnlocked())) {
      return "Награда уже разблокирована — зафиксируйте результат и поддерживайте ритм короткими сессиями.";
    }
    if (today.riskStatus() == RiskStatus.HIGH) {
      return "Цель в зоне риска. Снизьте перегрузку: сделайте короткую Pomodoro-сессию сегодня и обязательно завершите её фото-отчётом.";
    }
    if (today.remainingMinutesToday() != null && today.remainingMinutesToday() > 0) {
      return "Осталось %d минут до дневной нормы. Сейчас лучший шаг — закрыть ещё одну Pomodoro-сессию."
          .formatted(today.remainingMinutesToday());
    }
    if (today.reportStatusToday() == null || today.reportStatusToday() == ReportStatus.REJECTED) {
      return "После работы по цели не забудьте отправить фото-отчёт: без подтверждённого отчёта день не будет засчитан.";
    }
    if (forecast.remainingMinutes() != null
        && forecast.remainingMinutes() > 0
        && !Boolean.TRUE.equals(forecast.onTrack())) {
      return "Вы немного отстаёте от прогноза. Увеличьте средний ежедневный фокус хотя бы на одну короткую сессию.";
    }
    return "Вы движетесь стабильно. Сохраняйте текущий темп и не пропускайте вечерний фото-отчёт.";
  }

  private Optional<GoalCommitment> latestCommitment(Long goalId, Long userId) {
    return goalCommitmentRepository.findFirstByGoalIdAndUserIdOrderByCreatedAtDesc(goalId, userId);
  }

  private GoalDtos.GoalResponse toGoalResponse(Goal goal) {
    return new GoalDtos.GoalResponse(
        goal.getId(),
        goal.getTitle(),
        goal.getDescription(),
        goal.getTargetHours(),
        goal.getDeadline(),
        goal.getThemeColor(),
        goal.getStatus(),
        goal.getCurrentStreak(),
        goal.getCompletedAt(),
        goal.getClosedAt(),
        goal.getFailureReason(),
        goal.getCreatedAt());
  }

  private GoalExperienceDtos.CommitmentResponse toCommitmentResponse(GoalCommitment commitment) {
    return new GoalExperienceDtos.CommitmentResponse(
        commitment.getId(),
        commitment.getGoal().getId(),
        commitment.getDailyTargetMinutes(),
        commitment.getStartDate(),
        commitment.getEndDate(),
        commitment.getStatus(),
        commitment.getDisciplineScore(),
        commitment.getCurrentStreak(),
        commitment.getBestStreak(),
        commitment.getCompletedDays(),
        commitment.getMissedDays(),
        commitment.getPersonalRewardTitle(),
        commitment.getPersonalRewardDescription(),
        commitment.getRewardUnlocked(),
        commitment.getRiskStatus(),
        commitment.getCreatedAt(),
        commitment.getUpdatedAt());
  }

  private int focusMinutesForDate(Long goalId, LocalDate date) {
    OffsetDateTime start = date.atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
    OffsetDateTime end = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
    return focusSessionRepository.findByGoalIdAndStartedAtBetween(goalId, start, end).stream()
        .map(FocusSession::getDurationMinutes)
        .filter(java.util.Objects::nonNull)
        .mapToInt(Integer::intValue)
        .sum();
  }

  private ReportStatus resolveReportStatus(Long goalId, LocalDate date) {
    List<Report> reports = reportRepository.findByGoalIdAndReportDate(goalId, date);
    if (reports.stream().anyMatch(report -> report.getStatus() == ReportStatus.CONFIRMED)) {
      return ReportStatus.CONFIRMED;
    }
    if (reports.stream().anyMatch(report -> report.getStatus() == ReportStatus.PENDING)) {
      return ReportStatus.PENDING;
    }
    if (reports.stream().anyMatch(report -> report.getStatus() == ReportStatus.REJECTED)) {
      return ReportStatus.REJECTED;
    }
    if (reports.stream().anyMatch(report -> report.getStatus() == ReportStatus.OVERDUE)) {
      return ReportStatus.OVERDUE;
    }
    return null;
  }

  private String missedDayReason(
      int focusMinutes, GoalCommitment commitment, ReportStatus reportStatus) {
    if (focusMinutes < commitment.getDailyTargetMinutes()
        && reportStatus != ReportStatus.CONFIRMED) {
      return "не хватило минут фокуса и нет подтверждённого отчёта";
    }
    if (focusMinutes < commitment.getDailyTargetMinutes()) {
      return "не выполнена дневная норма по фокусу";
    }
    if (reportStatus == null) {
      return "не отправлен фото-отчёт";
    }
    return "отчёт не был подтверждён AI";
  }

  private int plannedDays(GoalCommitment commitment) {
    if (commitment.getEndDate() == null) {
      return Math.max(1, commitment.getCompletedDays() + commitment.getMissedDays());
    }
    long days = ChronoUnit.DAYS.between(commitment.getStartDate(), commitment.getEndDate()) + 1L;
    return (int) Math.max(days, 1L);
  }

  private String rewardDescription(GoalCommitment commitment) {
    if (commitment.getPersonalRewardTitle() == null) {
      return "Пользователь выполнил обязательство и заслужил личную награду.";
    }
    return "Разблокирована награда: %s".formatted(commitment.getPersonalRewardTitle());
  }

  private String blankToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

  private Double roundAverage(double value) {
    return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
  }
}
