package com.pomodoro.app.entity;

import com.pomodoro.app.enums.ChatRole;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.*;

@Entity
@Table(name = "chat_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "thread_id")
  private ChatThread thread;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ChatRole role;

  @Column(nullable = false, length = 6000)
  private String content;

  @Column(nullable = false)
  private OffsetDateTime createdAt;
}
