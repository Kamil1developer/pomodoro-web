package com.pomodoro.app.dto;

import com.pomodoro.app.enums.WalletStatus;
import com.pomodoro.app.enums.WalletTransactionType;
import java.time.OffsetDateTime;
import java.util.List;

public class WalletDtos {
  public record WalletResponse(
      Integer balance,
      Integer initialBalance,
      Integer totalAdded,
      Integer totalPenalties,
      WalletStatus status,
      OffsetDateTime createdAt,
      OffsetDateTime updatedAt) {}

  public record WalletTransactionResponse(
      Long id,
      WalletTransactionType type,
      Integer amount,
      Integer balanceBefore,
      Integer balanceAfter,
      String reason,
      String goalTitle,
      OffsetDateTime createdAt) {}

  public record WalletTransactionHistoryResponse(List<WalletTransactionResponse> transactions) {}
}
