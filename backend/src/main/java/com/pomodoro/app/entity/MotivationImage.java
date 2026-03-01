package com.pomodoro.app.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.*;

@Entity
@Table(name = "motivation_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MotivationImage {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "goal_id")
  private Goal goal;

  @Column(nullable = false)
  private String imagePath;

  @Column(nullable = false, length = 3000)
  private String prompt;

  @Column(nullable = false)
  private Boolean isFavorite;

  @Column(nullable = false)
  private OffsetDateTime createdAt;
}
