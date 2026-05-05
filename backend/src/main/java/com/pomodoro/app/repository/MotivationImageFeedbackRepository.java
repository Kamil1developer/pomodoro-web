package com.pomodoro.app.repository;

import com.pomodoro.app.entity.MotivationImageFeedback;
import com.pomodoro.app.enums.MotivationImageFeedbackType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MotivationImageFeedbackRepository
    extends JpaRepository<MotivationImageFeedback, Long> {
  boolean existsByUserIdAndImageIdAndType(
      Long userId, Long imageId, MotivationImageFeedbackType type);

  boolean existsByUserIdAndImageId(Long userId, Long imageId);

  long countByImageIdAndType(Long imageId, MotivationImageFeedbackType type);

  List<MotivationImageFeedback> findByUserId(Long userId);

  List<MotivationImageFeedback> findByImageId(Long imageId);
}
