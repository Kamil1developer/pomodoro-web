package com.pomodoro.app.service;

import com.pomodoro.app.dto.ProfileDtos;
import com.pomodoro.app.entity.FocusSession;
import com.pomodoro.app.entity.Goal;
import com.pomodoro.app.entity.GoalCommitment;
import com.pomodoro.app.entity.User;
import com.pomodoro.app.entity.UserWallet;
import com.pomodoro.app.enums.GoalStatus;
import com.pomodoro.app.enums.RiskStatus;
import com.pomodoro.app.exception.AppException;
import com.pomodoro.app.repository.FocusSessionRepository;
import com.pomodoro.app.repository.GoalCommitmentRepository;
import com.pomodoro.app.repository.GoalRepository;
import com.pomodoro.app.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProfileService {
  private static final long MAX_AVATAR_BYTES = 3L * 1024L * 1024L;
  private static final Set<String> ALLOWED_AVATAR_TYPES =
      Set.of("image/jpeg", "image/png", "image/webp");
  private static final Set<String> ALLOWED_AVATAR_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

  private final UserRepository userRepository;
  private final GoalRepository goalRepository;
  private final GoalCommitmentRepository goalCommitmentRepository;
  private final FocusSessionRepository focusSessionRepository;
  private final GoalCommitmentService goalCommitmentService;
  private final StorageService storageService;
  private final WalletService walletService;

  public record AvatarContent(byte[] bytes, String contentType) {}

  public ProfileService(
      UserRepository userRepository,
      GoalRepository goalRepository,
      GoalCommitmentRepository goalCommitmentRepository,
      FocusSessionRepository focusSessionRepository,
      GoalCommitmentService goalCommitmentService,
      StorageService storageService,
      WalletService walletService) {
    this.userRepository = userRepository;
    this.goalRepository = goalRepository;
    this.goalCommitmentRepository = goalCommitmentRepository;
    this.focusSessionRepository = focusSessionRepository;
    this.goalCommitmentService = goalCommitmentService;
    this.storageService = storageService;
    this.walletService = walletService;
  }

  @Transactional
  public ProfileDtos.ProfileResponse getProfile(Long userId) {
    User user = loadUser(userId);
    List<Goal> activeGoals =
        goalRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, GoalStatus.ACTIVE);
    List<Goal> historyGoals =
        goalRepository.findByUserIdAndStatusInOrderByCreatedAtDesc(
            userId, List.of(GoalStatus.COMPLETED, GoalStatus.FAILED));

    Map<Long, GoalCommitment> latestCommitments = latestCommitmentsByGoal(userId);
    List<FocusSession> allSessions =
        focusSessionRepository.findByGoalUserIdOrderByStartedAtDesc(userId);
    UserWallet wallet = walletService.getOrCreateWallet(userId);

    List<ProfileDtos.ProfileGoalItem> activeGoalItems = new ArrayList<>();
    for (Goal goal : activeGoals) {
      GoalCommitment commitment = latestCommitments.get(goal.getId());
      var today = goalCommitmentService.getTodayStatus(userId, goal.getId());
      activeGoalItems.add(
          new ProfileDtos.ProfileGoalItem(
              goal.getId(),
              goal.getTitle(),
              goal.getStatus(),
              goal.getCurrentStreak(),
              commitment != null ? commitment.getDailyTargetMinutes() : null,
              today.completedFocusMinutesToday(),
              today.remainingMinutesToday(),
              commitment != null ? commitment.getDisciplineScore() : null,
              commitment != null ? commitment.getRiskStatus() : null,
              today.moneyEnabled(),
              today.dailyPenaltyAmount() == null ? 10 : today.dailyPenaltyAmount(),
              commitment != null ? commitment.getTotalPenaltyCharged() : 0,
              today.moneyStatus() != null ? today.moneyStatus().name() : "ACTIVE",
              goal.getCreatedAt()));
    }

    List<ProfileDtos.ProfileGoalHistoryItem> historyItems =
        historyGoals.stream()
            .map(
                goal ->
                    new ProfileDtos.ProfileGoalHistoryItem(
                        goal.getId(),
                        goal.getTitle(),
                        goal.getStatus(),
                        goal.getFailureReason(),
                        goal.getCreatedAt(),
                        goal.getCompletedAt(),
                        goal.getClosedAt(),
                        Optional.ofNullable(latestCommitments.get(goal.getId()))
                            .map(GoalCommitment::getTotalPenaltyCharged)
                            .orElse(0),
                        goal.getStatus() == GoalStatus.FAILED))
            .toList();

    ProfileDtos.ProfileStatsResponse stats =
        buildStats(userId, activeGoals, historyGoals, latestCommitments, allSessions);

    return new ProfileDtos.ProfileResponse(
        user.getId(),
        user.getEmail(),
        user.getFullName(),
        user.getAvatarPath(),
        stats,
        new ProfileDtos.ProfileWalletResponse(
            wallet.getBalance(),
            wallet.getInitialBalance(),
            wallet.getTotalPenalties(),
            wallet.getStatus()),
        activeGoalItems,
        historyItems);
  }

  @Transactional
  public ProfileDtos.ProfileGoalsResponse getProfileGoals(Long userId) {
    ProfileDtos.ProfileResponse profile = getProfile(userId);
    return new ProfileDtos.ProfileGoalsResponse(profile.activeGoals(), profile.goalHistory());
  }

  @Transactional
  public ProfileDtos.ProfileResponse updateProfile(
      Long userId, ProfileDtos.UpdateProfileRequest request) {
    User user = loadUser(userId);
    if (request.fullName() != null && !request.fullName().isBlank()) {
      user.setFullName(request.fullName().trim());
    }
    userRepository.save(user);
    return getProfile(userId);
  }

  @Transactional
  public ProfileDtos.ProfileResponse uploadAvatar(Long userId, MultipartFile file) {
    validateAvatar(file);
    User user = loadUser(userId);
    String previousAvatar = user.getAvatarPath();
    String avatarPath = storageService.storeAvatar(file);
    user.setAvatarPath(avatarPath);
    userRepository.save(user);
    if (previousAvatar != null && !previousAvatar.isBlank()) {
      storageService.deletePublicPath(previousAvatar);
    }
    return getProfile(userId);
  }

  @Transactional(readOnly = true)
  public AvatarContent getAvatarContent(Long userId) {
    User user = loadUser(userId);
    if (user.getAvatarPath() == null || user.getAvatarPath().isBlank()) {
      throw new AppException(HttpStatus.NOT_FOUND, "Аватар ещё не загружен.");
    }
    Path path = storageService.resolvePublicPath(user.getAvatarPath());
    if (path == null || !Files.isRegularFile(path) || !Files.isReadable(path)) {
      throw new AppException(HttpStatus.NOT_FOUND, "Файл аватара не найден.");
    }
    try {
      String contentType = Files.probeContentType(path);
      if (contentType == null || !contentType.startsWith("image/")) {
        contentType = "application/octet-stream";
      }
      return new AvatarContent(Files.readAllBytes(path), contentType);
    } catch (IOException e) {
      throw new AppException(HttpStatus.NOT_FOUND, "Не удалось прочитать файл аватара.");
    }
  }

  private ProfileDtos.ProfileStatsResponse buildStats(
      Long userId,
      List<Goal> activeGoals,
      List<Goal> historyGoals,
      Map<Long, GoalCommitment> latestCommitments,
      List<FocusSession> allSessions) {
    int totalFocusMinutes =
        allSessions.stream()
            .map(FocusSession::getDurationMinutes)
            .filter(java.util.Objects::nonNull)
            .mapToInt(Integer::intValue)
            .sum();
    long completedGoalsCount =
        historyGoals.stream().filter(goal -> goal.getStatus() == GoalStatus.COMPLETED).count();
    long failedGoalsCount =
        historyGoals.stream().filter(goal -> goal.getStatus() == GoalStatus.FAILED).count();
    int bestStreak =
        latestCommitments.values().stream()
            .map(GoalCommitment::getBestStreak)
            .max(Integer::compareTo)
            .orElse(0);
    List<Integer> disciplineScores =
        latestCommitments.values().stream().map(GoalCommitment::getDisciplineScore).toList();
    Double averageDiscipline =
        disciplineScores.isEmpty()
            ? null
            : BigDecimal.valueOf(
                    disciplineScores.stream().mapToInt(Integer::intValue).average().orElse(0))
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();
    RiskStatus riskSummary =
        summarizeRisk(
            latestCommitments.values().stream().map(GoalCommitment::getRiskStatus).toList());

    return new ProfileDtos.ProfileStatsResponse(
        activeGoals.size(),
        completedGoalsCount,
        failedGoalsCount,
        totalFocusMinutes,
        bestStreak,
        averageDiscipline,
        riskSummary);
  }

  private RiskStatus summarizeRisk(List<RiskStatus> statuses) {
    if (statuses.stream().anyMatch(status -> status == RiskStatus.HIGH)) {
      return RiskStatus.HIGH;
    }
    if (statuses.stream().anyMatch(status -> status == RiskStatus.MEDIUM)) {
      return RiskStatus.MEDIUM;
    }
    return statuses.isEmpty() ? null : RiskStatus.LOW;
  }

  private Map<Long, GoalCommitment> latestCommitmentsByGoal(Long userId) {
    Map<Long, GoalCommitment> result = new HashMap<>();
    List<GoalCommitment> commitments =
        goalCommitmentRepository.findByUserIdOrderByCreatedAtDesc(userId);
    for (GoalCommitment commitment : commitments) {
      result.putIfAbsent(commitment.getGoal().getId(), commitment);
    }
    return result;
  }

  private void validateAvatar(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new AppException(HttpStatus.BAD_REQUEST, "Выберите изображение для аватара.");
    }
    if (file.getSize() > MAX_AVATAR_BYTES) {
      throw new AppException(HttpStatus.BAD_REQUEST, "Аватар слишком большой. Допустимо до 3 МБ.");
    }
    String contentType = file.getContentType();
    if (contentType == null
        || !ALLOWED_AVATAR_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
      throw new AppException(
          HttpStatus.BAD_REQUEST, "Аватар должен быть в формате JPG, PNG или WEBP.");
    }
    String originalName =
        file.getOriginalFilename() == null
            ? ""
            : file.getOriginalFilename().toLowerCase(Locale.ROOT);
    String extension =
        originalName.contains(".") ? originalName.substring(originalName.lastIndexOf('.') + 1) : "";
    if (!ALLOWED_AVATAR_EXTENSIONS.contains(extension)) {
      throw new AppException(HttpStatus.BAD_REQUEST, "Недопустимое расширение файла аватара.");
    }
  }

  private User loadUser(Long userId) {
    return userRepository
        .findById(userId)
        .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Пользователь не найден."));
  }
}
