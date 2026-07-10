package com.example.backtemplate.admin.dto;

import com.example.backtemplate.auth.User;
import java.util.UUID;

public record UserSummaryResponse(UUID id, String email, String role) {

  public static UserSummaryResponse from(User user) {
    return new UserSummaryResponse(user.getId(), user.getEmail(), user.getRole().name());
  }
}
