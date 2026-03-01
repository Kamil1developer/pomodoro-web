package com.pomodoro.app.controller;

import com.pomodoro.app.dto.AuthDtos;
import com.pomodoro.app.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/register")
  public AuthDtos.TokenResponse register(@RequestBody @Valid AuthDtos.RegisterRequest request) {
    return authService.register(request);
  }

  @PostMapping("/login")
  public AuthDtos.TokenResponse login(@RequestBody @Valid AuthDtos.LoginRequest request) {
    return authService.login(request);
  }

  @PostMapping("/refresh")
  public AuthDtos.TokenResponse refresh(@RequestBody @Valid AuthDtos.RefreshRequest request) {
    return authService.refresh(request);
  }

  @PostMapping("/logout")
  public void logout(@RequestBody @Valid AuthDtos.LogoutRequest request) {
    authService.logout(request);
  }
}
