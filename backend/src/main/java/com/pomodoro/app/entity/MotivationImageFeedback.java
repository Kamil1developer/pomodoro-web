package com.pomodoro.app.entity;

import com.pomodoro.app.enums.MotivationImageFeedbackType;
import com.pomodoro.app.enums.MotivationImageReportReason;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.*;

@Entity
@Table(
    name = "motivation_image_feedbacks",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"user_id", "image_id", "type"})})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MotivationImageFeedback {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id")
  private User user;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "image_id")
  private MotivationImage image;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private MotivationImageFeedbackType type;

  @Enumerated(EnumType.STRING)
  @Column(length = 32)
  private MotivationImageReportReason reason;

  @Column(length = 1000)
  private String comment;

  @Column(nullable = false)
  private OffsetDateTime createdAt;
}
