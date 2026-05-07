package com.pomodoro.app.repository;

import com.pomodoro.app.entity.WalletTransaction;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
  List<WalletTransaction> findByUserIdOrderByCreatedAtDesc(Long userId);
}
