package com.pomodoro.app.repository;

import com.pomodoro.app.entity.MotivationQuote;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MotivationQuoteRepository extends JpaRepository<MotivationQuote, Long> {
  Optional<MotivationQuote> findByGoalIdAndQuoteDate(Long goalId, LocalDate quoteDate);

  Optional<MotivationQuote> findFirstByGoalIdOrderByQuoteDateDesc(Long goalId);
}
