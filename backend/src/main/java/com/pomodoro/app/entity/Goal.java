package com.pomodoro.app.entity;

import com.pomodoro.app.enums.GoalStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.*;

@Entity
@Table(name = "goals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Goal {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id")
  private User user;

  @Column(nullable = false)
  private String title;

  @Column(length = 3000)
  private String description;

  private BigDecimal targetHours;

  private LocalDate deadline;

  @Column(nullable = false)
  private Integer currentStreak;

  @Column(nullable = false, length = 16)
  private String themeColor;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private GoalStatus status;

  private OffsetDateTime completedAt;

  private OffsetDateTime closedAt;

  @Column(length = 3000)
  private String failureReason;

  @Column(nullable = false)
  private OffsetDateTime createdAt;
}
