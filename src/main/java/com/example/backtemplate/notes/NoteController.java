package com.example.backtemplate.notes;

import com.example.backtemplate.notes.dto.NoteRequest;
import com.example.backtemplate.notes.dto.NoteResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class NoteController {

  private final NoteService noteService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public NoteResponse create(
      @AuthenticationPrincipal String ownerIdStr, @Valid @RequestBody NoteRequest req) {
    return noteService.create(UUID.fromString(ownerIdStr), req);
  }

  @GetMapping
  public List<NoteResponse> list(@AuthenticationPrincipal String ownerIdStr) {
    return noteService.list(UUID.fromString(ownerIdStr));
  }

  @GetMapping("/{id}")
  public NoteResponse get(@AuthenticationPrincipal String ownerIdStr, @PathVariable UUID id) {
    return noteService.get(UUID.fromString(ownerIdStr), id);
  }

  @PutMapping("/{id}")
  public NoteResponse update(
      @AuthenticationPrincipal String ownerIdStr,
      @PathVariable UUID id,
      @Valid @RequestBody NoteRequest req) {
    return noteService.update(UUID.fromString(ownerIdStr), id, req);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@AuthenticationPrincipal String ownerIdStr, @PathVariable UUID id) {
    noteService.delete(UUID.fromString(ownerIdStr), id);
  }
}
