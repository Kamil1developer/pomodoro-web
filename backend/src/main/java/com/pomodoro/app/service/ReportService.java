package com.pomodoro.app.service;

import com.pomodoro.app.dto.AiDtos;
import com.pomodoro.app.dto.ReportDtos;
import com.pomodoro.app.entity.Goal;
import com.pomodoro.app.entity.Report;
import com.pomodoro.app.enums.AiVerdict;
import com.pomodoro.app.enums.ReportStatus;
import com.pomodoro.app.repository.ReportRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ReportService {
  private final GoalService goalService;
  private final ReportRepository reportRepository;
  private final StorageService storageService;
  private final AiService aiService;

  public ReportService(
      GoalService goalService,
      ReportRepository reportRepository,
      StorageService storageService,
      AiService aiService) {
    this.goalService = goalService;
    this.reportRepository = reportRepository;
    this.storageService = storageService;
    this.aiService = aiService;
  }

  public ReportDtos.ReportResponse create(
      Long userId, Long goalId, MultipartFile file, String comment) {
    Goal goal = goalService.ownedGoal(userId, goalId);
    String path = storageService.storeReport(file);
    AiDtos.AnalyzeResult result;
    try {
      result =
          aiService.analyzeReportImage(
              file.getBytes(),
              comment,
              new AiDtos.GoalContext(goal.getId(), goal.getTitle(), goal.getDescription()));
    } catch (Exception e) {
      result =
          new AiDtos.AnalyzeResult(
              AiVerdict.NEEDS_MORE_INFO, 0.3, "AI analysis failed, try again.");
    }
    ReportStatus status =
        switch (result.verdict()) {
          case APPROVED -> ReportStatus.CONFIRMED;
          case REJECTED -> ReportStatus.REJECTED;
          case NEEDS_MORE_INFO -> ReportStatus.PENDING;
        };

    Report report =
        reportRepository.save(
            Report.builder()
                .goal(goal)
                .reportDate(LocalDate.now())
                .comment(comment)
                .imagePath(path)
                .status(status)
                .aiVerdict(result.verdict())
                .aiExplanation(result.explanation())
                .createdAt(OffsetDateTime.now())
                .build());

    return toResponse(report);
  }

  public List<ReportDtos.ReportResponse> list(Long userId, Long goalId) {
    goalService.ownedGoal(userId, goalId);
    return reportRepository.findByGoalIdOrderByCreatedAtDesc(goalId).stream()
        .map(this::toResponse)
        .toList();
  }

  public ReportDtos.ReportResponse toResponse(Report report) {
    return new ReportDtos.ReportResponse(
        report.getId(),
        report.getGoal().getId(),
        report.getReportDate(),
        report.getComment(),
        report.getImagePath(),
        report.getStatus(),
        report.getAiVerdict(),
        report.getAiExplanation(),
        report.getCreatedAt());
  }
}
