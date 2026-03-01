package com.pomodoro.app.repository;

import com.pomodoro.app.entity.MotivationImage;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MotivationImageRepository extends JpaRepository<MotivationImage, Long> {
  List<MotivationImage> findByGoalIdOrderByCreatedAtDesc(Long goalId);

  Optional<MotivationImage> findByIdAndGoalUserId(Long id, Long userId);
}
