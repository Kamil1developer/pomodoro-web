package com.pomodoro.app.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.*;

@Entity
@Table(
    name = "motivation_quotes",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"goal_id", "quote_date"})})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MotivationQuote {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "goal_id")
  private Goal goal;

  @Column(nullable = false, length = 2000)
  private String quoteText;

  @Column(nullable = false)
  private String quoteAuthor;

  @Column(nullable = false)
  private LocalDate quoteDate;

  @Column(nullable = false)
  private OffsetDateTime createdAt;
}
