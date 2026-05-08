package com.pomodoro.app.repository;

import com.pomodoro.app.entity.WalletTransaction;
import com.pomodoro.app.enums.WalletTransactionType;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
  List<WalletTransaction> findByUserIdOrderByCreatedAtDesc(Long userId);

  boolean existsByGoalIdAndTypeAndPenaltyDate(
      Long goalId, WalletTransactionType type, LocalDate penaltyDate);
}
