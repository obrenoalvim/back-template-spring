package com.example.backtemplate.account;

import com.example.backtemplate.account.dto.ChangePasswordRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "account")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {

  private final AccountService accountService;

  @Operation(summary = "Change the current user password")
  @PatchMapping("/password")
  public void changePassword(
      @AuthenticationPrincipal String userIdStr, @Valid @RequestBody ChangePasswordRequest req) {
    accountService.changePassword(UUID.fromString(userIdStr), req);
  }

  @Operation(summary = "Delete the current user account")
  @DeleteMapping
  public void deleteAccount(@AuthenticationPrincipal String userIdStr) {
    accountService.deleteAccount(UUID.fromString(userIdStr));
  }
}
