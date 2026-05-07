package com.pomodoro.app.entity;

import com.pomodoro.app.enums.WalletTransactionType;
import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "wallet_transactions")
public class WalletTransaction {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "wallet_id", nullable = false)
  private UserWallet wallet;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "goal_id")
  private Goal goal;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "commitment_id")
  private GoalCommitment commitment;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private WalletTransactionType type;

  @Column(nullable = false)
  private Integer amount;

  @Column(nullable = false)
  private Integer balanceBefore;

  @Column(nullable = false)
  private Integer balanceAfter;

  @Column(length = 1000)
  private String reason;

  @Column(nullable = false)
  private OffsetDateTime createdAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public UserWallet getWallet() {
    return wallet;
  }

  public void setWallet(UserWallet wallet) {
    this.wallet = wallet;
  }

  public Goal getGoal() {
    return goal;
  }

  public void setGoal(Goal goal) {
    this.goal = goal;
  }

  public GoalCommitment getCommitment() {
    return commitment;
  }

  public void setCommitment(GoalCommitment commitment) {
    this.commitment = commitment;
  }

  public WalletTransactionType getType() {
    return type;
  }

  public void setType(WalletTransactionType type) {
    this.type = type;
  }

  public Integer getAmount() {
    return amount;
  }

  public void setAmount(Integer amount) {
    this.amount = amount;
  }

  public Integer getBalanceBefore() {
    return balanceBefore;
  }

  public void setBalanceBefore(Integer balanceBefore) {
    this.balanceBefore = balanceBefore;
  }

  public Integer getBalanceAfter() {
    return balanceAfter;
  }

  public void setBalanceAfter(Integer balanceAfter) {
    this.balanceAfter = balanceAfter;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }
}
