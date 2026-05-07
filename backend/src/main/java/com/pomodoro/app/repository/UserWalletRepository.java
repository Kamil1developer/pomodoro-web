package com.pomodoro.app.repository;

import com.pomodoro.app.entity.UserWallet;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserWalletRepository extends JpaRepository<UserWallet, Long> {
  Optional<UserWallet> findByUserId(Long userId);
}
