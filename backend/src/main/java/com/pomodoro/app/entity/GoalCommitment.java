package com.pomodoro.app.entity;

import com.pomodoro.app.enums.CommitmentMoneyStatus;
import com.pomodoro.app.enums.CommitmentStatus;
import com.pomodoro.app.enums.RiskStatus;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "goal_commitments")
public class GoalCommitment {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "goal_id", nullable = false)
  private Goal goal;

  @Column(nullable = false)
  private Integer dailyTargetMinutes;

  @Column(nullable = false)
  private LocalDate startDate;

  private LocalDate endDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private CommitmentStatus status;

  @Column(nullable = false)
  private Integer disciplineScore;

  @Column(nullable = false)
  private Integer currentStreak;

  @Column(nullable = false)
  private Integer bestStreak;

  @Column(nullable = false)
  private Integer completedDays;

  @Column(nullable = false)
  private Integer missedDays;

  private String personalRewardTitle;

  @Column(length = 3000)
  private String personalRewardDescription;

  @Column(nullable = false)
  private Boolean rewardUnlocked;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private RiskStatus riskStatus;

  @Column(nullable = false)
  private Integer depositAmount = 300;

  @Column(nullable = false)
  private Integer dailyPenaltyAmount = 10;

  @Column(nullable = false)
  private Integer totalPenaltyCharged = 0;

  @Column(nullable = false)
  private Boolean moneyEnabled = true;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private CommitmentMoneyStatus moneyStatus = CommitmentMoneyStatus.ACTIVE;

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

  public Goal getGoal() {
    return goal;
  }

  public void setGoal(Goal goal) {
    this.goal = goal;
  }

  public Integer getDailyTargetMinutes() {
    return dailyTargetMinutes;
  }

  public void setDailyTargetMinutes(Integer dailyTargetMinutes) {
    this.dailyTargetMinutes = dailyTargetMinutes;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public void setStartDate(LocalDate startDate) {
    this.startDate = startDate;
  }

  public LocalDate getEndDate() {
    return endDate;
  }

  public void setEndDate(LocalDate endDate) {
    this.endDate = endDate;
  }

  public CommitmentStatus getStatus() {
    return status;
  }

  public void setStatus(CommitmentStatus status) {
    this.status = status;
  }

  public Integer getDisciplineScore() {
    return disciplineScore;
  }

  public void setDisciplineScore(Integer disciplineScore) {
    this.disciplineScore = disciplineScore;
  }

  public Integer getCurrentStreak() {
    return currentStreak;
  }

  public void setCurrentStreak(Integer currentStreak) {
    this.currentStreak = currentStreak;
  }

  public Integer getBestStreak() {
    return bestStreak;
  }

  public void setBestStreak(Integer bestStreak) {
    this.bestStreak = bestStreak;
  }

  public Integer getCompletedDays() {
    return completedDays;
  }

  public void setCompletedDays(Integer completedDays) {
    this.completedDays = completedDays;
  }

  public Integer getMissedDays() {
    return missedDays;
  }

  public void setMissedDays(Integer missedDays) {
    this.missedDays = missedDays;
  }

  public String getPersonalRewardTitle() {
    return personalRewardTitle;
  }

  public void setPersonalRewardTitle(String personalRewardTitle) {
    this.personalRewardTitle = personalRewardTitle;
  }

  public String getPersonalRewardDescription() {
    return personalRewardDescription;
  }

  public void setPersonalRewardDescription(String personalRewardDescription) {
    this.personalRewardDescription = personalRewardDescription;
  }

  public Boolean getRewardUnlocked() {
    return rewardUnlocked;
  }

  public void setRewardUnlocked(Boolean rewardUnlocked) {
    this.rewardUnlocked = rewardUnlocked;
  }

  public RiskStatus getRiskStatus() {
    return riskStatus;
  }

  public void setRiskStatus(RiskStatus riskStatus) {
    this.riskStatus = riskStatus;
  }

  public Integer getDepositAmount() {
    return depositAmount;
  }

  public void setDepositAmount(Integer depositAmount) {
    this.depositAmount = depositAmount;
  }

  public Integer getDailyPenaltyAmount() {
    return dailyPenaltyAmount;
  }

  public void setDailyPenaltyAmount(Integer dailyPenaltyAmount) {
    this.dailyPenaltyAmount = dailyPenaltyAmount;
  }

  public Integer getTotalPenaltyCharged() {
    return totalPenaltyCharged;
  }

  public void setTotalPenaltyCharged(Integer totalPenaltyCharged) {
    this.totalPenaltyCharged = totalPenaltyCharged;
  }

  public Boolean getMoneyEnabled() {
    return moneyEnabled;
  }

  public void setMoneyEnabled(Boolean moneyEnabled) {
    this.moneyEnabled = moneyEnabled;
  }

  public CommitmentMoneyStatus getMoneyStatus() {
    return moneyStatus;
  }

  public void setMoneyStatus(CommitmentMoneyStatus moneyStatus) {
    this.moneyStatus = moneyStatus;
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
