package com.pomodoro.app.entity;

import com.pomodoro.app.enums.MotivationImageSource;
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

  @Column(nullable = false)
  private String sourceUrl;

  @Column(nullable = false)
  private String title;

  @Column(length = 3000)
  private String description;

  @Column(nullable = false, length = 64)
  private String theme;

  @Column(nullable = false, length = 3000)
  private String prompt;

  @Column(nullable = false)
  private Boolean isFavorite;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private MotivationImageSource generatedBy;

  @Column(nullable = false)
  private Boolean hiddenGlobally;

  @Column(nullable = false)
  private Integer reportCount;

  private OffsetDateTime favoritedAt;

  @Column(nullable = false)
  private OffsetDateTime createdAt;
}
