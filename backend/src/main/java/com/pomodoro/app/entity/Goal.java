package com.pomodoro.app.entity;

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

  @Column(nullable = false)
  private OffsetDateTime createdAt;
}
