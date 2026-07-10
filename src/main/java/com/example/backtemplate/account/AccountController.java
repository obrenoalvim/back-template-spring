package com.example.backtemplate.account;

import com.example.backtemplate.account.dto.ChangePasswordRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {

  private final AccountService accountService;

  @PatchMapping("/password")
  public void changePassword(
      @AuthenticationPrincipal String userIdStr, @Valid @RequestBody ChangePasswordRequest req) {
    accountService.changePassword(UUID.fromString(userIdStr), req);
  }

  @DeleteMapping
  public void deleteAccount(@AuthenticationPrincipal String userIdStr) {
    accountService.deleteAccount(UUID.fromString(userIdStr));
  }
}
