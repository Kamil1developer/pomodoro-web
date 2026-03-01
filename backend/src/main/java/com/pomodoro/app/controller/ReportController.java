package com.pomodoro.app.controller;

import com.pomodoro.app.dto.ReportDtos;
import com.pomodoro.app.service.ReportService;
import com.pomodoro.app.util.AuthUtil;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/goals/{goalId}/reports")
public class ReportController {
  private final ReportService reportService;

  public ReportController(ReportService reportService) {
    this.reportService = reportService;
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ReportDtos.ReportResponse create(
      @PathVariable Long goalId,
      @RequestPart("file") MultipartFile file,
      @RequestPart(value = "comment", required = false) String comment) {
    return reportService.create(AuthUtil.currentUserId(), goalId, file, comment);
  }

  @GetMapping
  public List<ReportDtos.ReportResponse> list(@PathVariable Long goalId) {
    return reportService.list(AuthUtil.currentUserId(), goalId);
  }
}
