package com.pomodoro.app.entity;

import com.pomodoro.app.enums.AiVerdict;
import com.pomodoro.app.enums.ReportStatus;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.*;

@Entity
@Table(name = "reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Report {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "goal_id")
  private Goal goal;

  @Column(nullable = false)
  private LocalDate reportDate;

  @Column(length = 3000)
  private String comment;

  @Column(nullable = false)
  private String imagePath;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ReportStatus status;

  @Enumerated(EnumType.STRING)
  private AiVerdict aiVerdict;

  private Double aiConfidence;

  @Column(length = 3000)
  private String aiExplanation;

  @Column(nullable = false)
  private OffsetDateTime createdAt;
}
