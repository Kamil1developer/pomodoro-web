package com.pomodoro.app.repository;

import com.pomodoro.app.entity.DailySummary;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailySummaryRepository extends JpaRepository<DailySummary, Long> {
  Optional<DailySummary> findByGoalIdAndSummaryDate(Long goalId, LocalDate summaryDate);

  List<DailySummary> findByGoalIdOrderBySummaryDateAsc(Long goalId);
}
