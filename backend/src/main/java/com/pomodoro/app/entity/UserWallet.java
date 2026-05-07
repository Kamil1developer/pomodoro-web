package com.pomodoro.app.entity;

import com.pomodoro.app.enums.WalletStatus;
import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "user_wallets")
public class UserWallet {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false, unique = true)
  private User user;

  @Column(nullable = false)
  private Integer balance;

  @Column(nullable = false)
  private Integer initialBalance;

  @Column(nullable = false)
  private Integer totalAdded;

  @Column(nullable = false)
  private Integer totalPenalties;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private WalletStatus status;

  @Column(nullable = false)
  private OffsetDateTime createdAt;

  @Column(nullable = false)
  private OffsetDateTime updatedAt;

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

  public Integer getBalance() {
    return balance;
  }

  public void setBalance(Integer balance) {
    this.balance = balance;
  }

  public Integer getInitialBalance() {
    return initialBalance;
  }

  public void setInitialBalance(Integer initialBalance) {
    this.initialBalance = initialBalance;
  }

  public Integer getTotalAdded() {
    return totalAdded;
  }

  public void setTotalAdded(Integer totalAdded) {
    this.totalAdded = totalAdded;
  }

  public Integer getTotalPenalties() {
    return totalPenalties;
  }

  public void setTotalPenalties(Integer totalPenalties) {
    this.totalPenalties = totalPenalties;
  }

  public WalletStatus getStatus() {
    return status;
  }

  public void setStatus(WalletStatus status) {
    this.status = status;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }
}
