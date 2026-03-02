package com.pomodoro.app.service;

import com.pomodoro.app.dto.AiDtos;
import com.pomodoro.app.dto.ReportDtos;
import com.pomodoro.app.entity.Goal;
import com.pomodoro.app.entity.Report;
import com.pomodoro.app.entity.TaskItem;
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
  private static final String TASK_BLOCK_START = "TODAY_TASKS_START";
  private static final String TASK_BLOCK_END = "TODAY_TASKS_END";

  private final GoalService goalService;
  private final DailyTaskPolicyService dailyTaskPolicyService;
  private final ReportRepository reportRepository;
  private final StorageService storageService;
  private final AiService aiService;

  public ReportService(
      GoalService goalService,
      DailyTaskPolicyService dailyTaskPolicyService,
      ReportRepository reportRepository,
      StorageService storageService,
      AiService aiService) {
    this.goalService = goalService;
    this.dailyTaskPolicyService = dailyTaskPolicyService;
    this.reportRepository = reportRepository;
    this.storageService = storageService;
    this.aiService = aiService;
  }

  public ReportDtos.ReportResponse create(
      Long userId, Long goalId, MultipartFile file, String comment) {
    dailyTaskPolicyService.ensureTasksCreatedToday(userId, goalId);
    Goal goal = goalService.ownedGoal(userId, goalId);
    List<TaskItem> todayTasks = dailyTaskPolicyService.todayTasksForGoal(goal);
    String path = storageService.storeReport(file);
    AiDtos.AnalyzeResult result;
    try {
      result =
          aiService.analyzeReportImage(
              file.getBytes(),
              comment == null ? "" : comment,
              new AiDtos.GoalContext(
                  goal.getId(),
                  goal.getTitle(),
                  buildAiGoalDescription(goal.getDescription(), todayTasks)));
    } catch (Exception e) {
      result =
          new AiDtos.AnalyzeResult(
              AiVerdict.NEEDS_MORE_INFO,
              0.3,
              "Не удалось выполнить AI-проверку. Повторите попытку позже.");
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

  private String buildAiGoalDescription(String goalDescription, List<TaskItem> todayTasks) {
    StringBuilder builder = new StringBuilder();
    if (goalDescription != null && !goalDescription.isBlank()) {
      builder.append("Цель: ").append(goalDescription.trim()).append("\n");
    } else {
      builder.append("Цель: без описания\n");
    }
    builder.append(TASK_BLOCK_START).append("\n");
    for (TaskItem task : todayTasks) {
      builder
          .append("- ")
          .append(task.getTitle())
          .append(task.getIsDone() ? " (статус: выполнено)" : " (статус: не выполнено)")
          .append("\n");
    }
    builder.append(TASK_BLOCK_END);
    return builder.toString();
  }
}
