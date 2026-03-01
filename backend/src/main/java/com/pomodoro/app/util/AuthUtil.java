package com.pomodoro.app.util;

import com.pomodoro.app.exception.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class AuthUtil {
  private AuthUtil() {}

  public static Long currentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !(authentication.getPrincipal() instanceof Long id)) {
      throw new AppException(HttpStatus.UNAUTHORIZED, "Unauthorized");
    }
    return id;
  }
}
