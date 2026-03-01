package com.pomodoro.app.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.*;

@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id")
  private User user;

  @Column(nullable = false, unique = true, length = 2000)
  private String token;

  @Column(nullable = false)
  private OffsetDateTime expiresAt;

  @Column(nullable = false)
  private OffsetDateTime createdAt;
}
