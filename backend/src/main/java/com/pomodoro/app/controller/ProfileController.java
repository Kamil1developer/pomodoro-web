package com.pomodoro.app.controller;

import com.pomodoro.app.dto.ProfileDtos;
import com.pomodoro.app.service.ProfileService;
import com.pomodoro.app.util.AuthUtil;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {
  private final ProfileService profileService;

  public ProfileController(ProfileService profileService) {
    this.profileService = profileService;
  }

  @GetMapping
  public ProfileDtos.ProfileResponse getProfile() {
    return profileService.getProfile(AuthUtil.currentUserId());
  }

  @PutMapping
  public ProfileDtos.ProfileResponse updateProfile(
      @RequestBody @Valid ProfileDtos.UpdateProfileRequest request) {
    return profileService.updateProfile(AuthUtil.currentUserId(), request);
  }

  @PostMapping(path = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ProfileDtos.ProfileResponse uploadAvatar(@RequestPart("file") MultipartFile file) {
    return profileService.uploadAvatar(AuthUtil.currentUserId(), file);
  }

  @GetMapping("/goals")
  public ProfileDtos.ProfileGoalsResponse getProfileGoals() {
    return profileService.getProfileGoals(AuthUtil.currentUserId());
  }
}
