package com.pomodoro.app.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.*;

@Entity
@Table(name = "tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskItem {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "goal_id")
  private Goal goal;

  @Column(nullable = false)
  private String title;

  @Column(nullable = false)
  private Boolean isDone;

  @Column(nullable = false)
  private OffsetDateTime createdAt;
}
