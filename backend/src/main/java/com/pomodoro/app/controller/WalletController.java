package com.pomodoro.app.controller;

import com.pomodoro.app.dto.WalletDtos;
import com.pomodoro.app.service.WalletService;
import com.pomodoro.app.util.AuthUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {
  private final WalletService walletService;

  public WalletController(WalletService walletService) {
    this.walletService = walletService;
  }

  @GetMapping
  public WalletDtos.WalletResponse getWallet() {
    return walletService.getWallet(AuthUtil.currentUserId());
  }

  @GetMapping("/transactions")
  public WalletDtos.WalletTransactionHistoryResponse getTransactions() {
    return walletService.getTransactionHistory(AuthUtil.currentUserId());
  }
}
