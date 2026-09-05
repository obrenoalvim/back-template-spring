package com.example.backtemplate.admin;

import com.example.backtemplate.admin.dto.NoteWithOwnerResponse;
import com.example.backtemplate.admin.dto.UserSummaryResponse;
import com.example.backtemplate.auth.UserRepository;
import com.example.backtemplate.notes.NoteRepository;
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
  private final NoteRepository noteRepository;

  @Operation(summary = "List all users (admin only)")
  @GetMapping("/users")
  public List<UserSummaryResponse> users() {
    return userRepository.findAll().stream().map(UserSummaryResponse::from).toList();
  }

  @Operation(summary = "List all notes with their owner (admin only)")
  @GetMapping("/notes")
  public List<NoteWithOwnerResponse> notes() {
    // findAllWithOwner() JOIN FETCHes the owner in one query — see
    // AdminControllerIntegrationTest#notes_endpoint_runs_a_constant_number_of_queries,
    // which pins that count so a future edit here can't regress into N+1.
    return noteRepository.findAllWithOwner().stream().map(NoteWithOwnerResponse::from).toList();
  }
}
