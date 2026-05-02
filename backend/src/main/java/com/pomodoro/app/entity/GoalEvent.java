package com.pomodoro.app.entity;

import com.pomodoro.app.enums.GoalEventType;
import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "goal_events")
public class GoalEvent {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "goal_id", nullable = false)
  private Goal goal;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "commitment_id")
  private GoalCommitment commitment;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private GoalEventType type;

  @Column(nullable = false)
  private String title;

  @Column(length = 3000)
  private String description;

  private String oldValue;

  private String newValue;

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

  public GoalEventType getType() {
    return type;
  }

  public void setType(GoalEventType type) {
    this.type = type;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getOldValue() {
    return oldValue;
  }

  public void setOldValue(String oldValue) {
    this.oldValue = oldValue;
  }

  public String getNewValue() {
    return newValue;
  }

  public void setNewValue(String newValue) {
    this.newValue = newValue;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }
}
