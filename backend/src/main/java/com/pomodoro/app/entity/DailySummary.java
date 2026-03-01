package com.pomodoro.app.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.*;

@Entity
@Table(name = "daily_summaries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailySummary {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "goal_id")
  private Goal goal;

  @Column(nullable = false)
  private LocalDate summaryDate;

  @Column(nullable = false)
  private Integer completedTasks;

  @Column(nullable = false)
  private Integer focusMinutes;

  @Column(nullable = false)
  private Integer streak;

  @Column(nullable = false)
  private OffsetDateTime createdAt;
}
