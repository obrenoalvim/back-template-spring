package com.example.backtemplate.admin;

import com.example.backtemplate.admin.dto.UserSummaryResponse;
import com.example.backtemplate.auth.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "admin")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

  private final UserRepository userRepository;

  @Operation(summary = "List all users (admin only)")
  @GetMapping("/users")
  public List<UserSummaryResponse> users() {
    return userRepository.findAll().stream().map(UserSummaryResponse::from).toList();
  }
}
