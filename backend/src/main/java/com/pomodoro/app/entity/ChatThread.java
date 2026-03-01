package com.pomodoro.app.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.*;

@Entity
@Table(name = "chat_threads")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatThread {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "goal_id")
  private Goal goal;

  @Column(nullable = false)
  private OffsetDateTime createdAt;
}
