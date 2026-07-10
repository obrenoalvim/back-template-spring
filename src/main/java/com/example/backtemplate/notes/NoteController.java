package com.example.backtemplate.notes;

import com.example.backtemplate.notes.dto.NoteRequest;
import com.example.backtemplate.notes.dto.NoteResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "notes")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class NoteController {

  private final NoteService noteService;

  @Operation(summary = "Create a note")
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public NoteResponse create(
      @AuthenticationPrincipal String ownerIdStr, @Valid @RequestBody NoteRequest req) {
    return noteService.create(UUID.fromString(ownerIdStr), req);
  }

  @Operation(summary = "List notes for the current user")
  @GetMapping
  public List<NoteResponse> list(@AuthenticationPrincipal String ownerIdStr) {
    return noteService.list(UUID.fromString(ownerIdStr));
  }

  @Operation(summary = "Get a note by id")
  @GetMapping("/{id}")
  public NoteResponse get(@AuthenticationPrincipal String ownerIdStr, @PathVariable UUID id) {
    return noteService.get(UUID.fromString(ownerIdStr), id);
  }

  @Operation(summary = "Update a note")
  @PutMapping("/{id}")
  public NoteResponse update(
      @AuthenticationPrincipal String ownerIdStr,
      @PathVariable UUID id,
      @Valid @RequestBody NoteRequest req) {
    return noteService.update(UUID.fromString(ownerIdStr), id, req);
  }

  @Operation(summary = "Delete a note")
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@AuthenticationPrincipal String ownerIdStr, @PathVariable UUID id) {
    noteService.delete(UUID.fromString(ownerIdStr), id);
  }
}
