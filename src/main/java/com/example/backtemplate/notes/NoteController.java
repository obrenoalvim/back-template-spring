package com.example.backtemplate.notes;

import com.example.backtemplate.notes.dto.NoteRequest;
import com.example.backtemplate.notes.dto.NoteResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;

    // TEMPORARY: X-Owner-Id header stands in for the authenticated principal
    // until Task 14 wires JwtAuthFilter + SecurityContext.

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NoteResponse create(@RequestHeader("X-Owner-Id") UUID ownerId, @Valid @RequestBody NoteRequest req) {
        return noteService.create(ownerId, req);
    }

    @GetMapping
    public List<NoteResponse> list(@RequestHeader("X-Owner-Id") UUID ownerId) {
        return noteService.list(ownerId);
    }

    @GetMapping("/{id}")
    public NoteResponse get(@RequestHeader("X-Owner-Id") UUID ownerId, @PathVariable UUID id) {
        return noteService.get(ownerId, id);
    }

    @PutMapping("/{id}")
    public NoteResponse update(
            @RequestHeader("X-Owner-Id") UUID ownerId,
            @PathVariable UUID id,
            @Valid @RequestBody NoteRequest req) {
        return noteService.update(ownerId, id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestHeader("X-Owner-Id") UUID ownerId, @PathVariable UUID id) {
        noteService.delete(ownerId, id);
    }
}
