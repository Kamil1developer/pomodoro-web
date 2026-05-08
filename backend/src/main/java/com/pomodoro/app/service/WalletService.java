package com.pomodoro.app.service;

import com.pomodoro.app.dto.WalletDtos;
import com.pomodoro.app.entity.Goal;
import com.pomodoro.app.entity.GoalCommitment;
import com.pomodoro.app.entity.User;
import com.pomodoro.app.entity.UserWallet;
import com.pomodoro.app.entity.WalletTransaction;
import com.pomodoro.app.enums.WalletStatus;
import com.pomodoro.app.enums.WalletTransactionType;
import com.pomodoro.app.exception.AppException;
import com.pomodoro.app.repository.UserRepository;
import com.pomodoro.app.repository.UserWalletRepository;
import com.pomodoro.app.repository.WalletTransactionRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletService {
  public static final int DEFAULT_INITIAL_BALANCE = 1000;

  private final UserWalletRepository userWalletRepository;
  private final WalletTransactionRepository walletTransactionRepository;
  private final UserRepository userRepository;

  public WalletService(
      UserWalletRepository userWalletRepository,
      WalletTransactionRepository walletTransactionRepository,
      UserRepository userRepository) {
    this.userWalletRepository = userWalletRepository;
    this.walletTransactionRepository = walletTransactionRepository;
    this.userRepository = userRepository;
  }

  @Transactional
  public UserWallet initializeWalletForNewUser(User user) {
    return userWalletRepository
        .findByUserId(user.getId())
        .orElseGet(
            () -> {
              OffsetDateTime now = OffsetDateTime.now();
              UserWallet wallet = new UserWallet();
              wallet.setUser(user);
              wallet.setBalance(DEFAULT_INITIAL_BALANCE);
              wallet.setInitialBalance(DEFAULT_INITIAL_BALANCE);
              wallet.setTotalAdded(DEFAULT_INITIAL_BALANCE);
              wallet.setTotalPenalties(0);
              wallet.setStatus(WalletStatus.ACTIVE);
              wallet.setCreatedAt(now);
              wallet.setUpdatedAt(now);
              UserWallet saved = userWalletRepository.save(wallet);
              addTransaction(
                  saved,
                  null,
                  null,
                  WalletTransactionType.INITIAL_GRANT,
                  DEFAULT_INITIAL_BALANCE,
                  0,
                  DEFAULT_INITIAL_BALANCE,
                  null,
                  "Стартовый баланс для виртуальной ответственности.");
              return saved;
            });
  }

  @Transactional
  public UserWallet getOrCreateWallet(Long userId) {
    return userWalletRepository
        .findByUserId(userId)
        .orElseGet(() -> initializeWalletForNewUser(loadUser(userId)));
  }

  @Transactional
  public WalletDtos.WalletResponse getWallet(Long userId) {
    UserWallet wallet = getOrCreateWallet(userId);
    return toWalletResponse(wallet);
  }

  @Transactional
  public WalletTransaction chargeDailyPenalty(
      Long userId, Goal goal, GoalCommitment commitment, int amount, String reason) {
    return chargeDailyPenalty(userId, goal, commitment, amount, null, reason);
  }

  @Transactional
  public Optional<WalletTransaction> chargeDailyPenaltyIfAbsent(
      Long userId,
      Goal goal,
      GoalCommitment commitment,
      int amount,
      LocalDate penaltyDate,
      String reason) {
    if (penaltyDate != null
        && goal != null
        && walletTransactionRepository.existsByGoalIdAndTypeAndPenaltyDate(
            goal.getId(), WalletTransactionType.DAILY_PENALTY, penaltyDate)) {
      return Optional.empty();
    }
    return Optional.of(chargeDailyPenalty(userId, goal, commitment, amount, penaltyDate, reason));
  }

  private WalletTransaction chargeDailyPenalty(
      Long userId,
      Goal goal,
      GoalCommitment commitment,
      int amount,
      LocalDate penaltyDate,
      String reason) {
    UserWallet wallet = getOrCreateWallet(userId);
    int normalizedAmount = Math.max(amount, 0);
    int balanceBefore = wallet.getBalance();
    int chargedAmount = Math.min(balanceBefore, normalizedAmount);
    int balanceAfter = Math.max(balanceBefore - chargedAmount, 0);

    wallet.setBalance(balanceAfter);
    wallet.setTotalPenalties(wallet.getTotalPenalties() + chargedAmount);
    wallet.setStatus(balanceAfter == 0 ? WalletStatus.EMPTY : WalletStatus.ACTIVE);
    wallet.setUpdatedAt(OffsetDateTime.now());
    userWalletRepository.save(wallet);

    WalletTransaction transaction =
        addTransaction(
            wallet,
            goal,
            commitment,
            WalletTransactionType.DAILY_PENALTY,
            chargedAmount,
            balanceBefore,
            balanceAfter,
            penaltyDate,
            reason);

    if (balanceAfter == 0 && balanceBefore > 0) {
      addTransaction(
          wallet,
          goal,
          commitment,
          WalletTransactionType.ACCOUNT_LOCKED,
          0,
          balanceAfter,
          balanceAfter,
          null,
          "Баланс виртуальной ответственности закончился.");
    }
    return transaction;
  }

  @Transactional(readOnly = true)
  public WalletDtos.WalletTransactionHistoryResponse getTransactionHistory(Long userId) {
    return new WalletDtos.WalletTransactionHistoryResponse(
        walletTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
            .map(this::toTransactionResponse)
            .toList());
  }

  private WalletTransaction addTransaction(
      UserWallet wallet,
      Goal goal,
      GoalCommitment commitment,
      WalletTransactionType type,
      int amount,
      int balanceBefore,
      int balanceAfter,
      LocalDate penaltyDate,
      String reason) {
    WalletTransaction transaction = new WalletTransaction();
    transaction.setUser(wallet.getUser());
    transaction.setWallet(wallet);
    transaction.setGoal(goal);
    transaction.setCommitment(commitment);
    transaction.setType(type);
    transaction.setAmount(Math.max(amount, 0));
    transaction.setBalanceBefore(Math.max(balanceBefore, 0));
    transaction.setBalanceAfter(Math.max(balanceAfter, 0));
    transaction.setPenaltyDate(penaltyDate);
    transaction.setReason(reason);
    transaction.setCreatedAt(OffsetDateTime.now());
    return walletTransactionRepository.save(transaction);
  }

  private WalletDtos.WalletResponse toWalletResponse(UserWallet wallet) {
    return new WalletDtos.WalletResponse(
        wallet.getBalance(),
        wallet.getInitialBalance(),
        wallet.getTotalAdded(),
        wallet.getTotalPenalties(),
        wallet.getStatus(),
        wallet.getCreatedAt(),
        wallet.getUpdatedAt());
  }

  private WalletDtos.WalletTransactionResponse toTransactionResponse(
      WalletTransaction transaction) {
    return new WalletDtos.WalletTransactionResponse(
        transaction.getId(),
        transaction.getType(),
        transaction.getAmount(),
        transaction.getBalanceBefore(),
        transaction.getBalanceAfter(),
        transaction.getReason(),
        transaction.getGoal() != null ? transaction.getGoal().getTitle() : null,
        transaction.getPenaltyDate(),
        transaction.getCreatedAt());
  }

  private User loadUser(Long userId) {
    return userRepository
        .findById(userId)
        .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Пользователь не найден."));
  }
}
