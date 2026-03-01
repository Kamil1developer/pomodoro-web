package com.pomodoro.app.repository;

import com.pomodoro.app.entity.Report;
import com.pomodoro.app.enums.ReportStatus;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {
  List<Report> findByGoalIdOrderByCreatedAtDesc(Long goalId);

  List<Report> findByStatusAndReportDateBefore(ReportStatus status, LocalDate date);

  List<Report> findByGoalIdAndReportDate(Long goalId, LocalDate reportDate);
}
