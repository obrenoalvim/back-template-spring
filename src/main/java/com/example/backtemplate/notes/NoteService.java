package com.example.backtemplate.notes;

import com.example.backtemplate.common.ApiException;
import com.example.backtemplate.notes.dto.NoteRequest;
import com.example.backtemplate.notes.dto.NoteResponse;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NoteService {

  private final NoteRepository noteRepository;

  public NoteResponse create(UUID ownerId, NoteRequest req) {
    Note note = new Note();
    note.setTitle(req.title());
    note.setContent(req.content());
    note.setOwnerId(ownerId);
    return NoteResponse.from(noteRepository.save(note));
  }

  public List<NoteResponse> list(UUID ownerId) {
    return noteRepository.findAllByOwnerId(ownerId).stream().map(NoteResponse::from).toList();
  }

  public NoteResponse get(UUID ownerId, UUID id) {
    return NoteResponse.from(findOwned(ownerId, id));
  }

  public NoteResponse update(UUID ownerId, UUID id, NoteRequest req) {
    Note note = findOwned(ownerId, id);
    note.setTitle(req.title());
    note.setContent(req.content());
    return NoteResponse.from(noteRepository.save(note));
  }

  public void delete(UUID ownerId, UUID id) {
    noteRepository.delete(findOwned(ownerId, id));
  }

  private Note findOwned(UUID ownerId, UUID id) {
    return noteRepository
        .findByIdAndOwnerId(id, ownerId)
        .orElseThrow(() -> ApiException.notFound("Note not found"));
  }
}
