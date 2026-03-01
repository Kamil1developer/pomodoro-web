package com.pomodoro.app.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record ErrorResponse(
    String code, String message, List<String> details, OffsetDateTime timestamp) {}
