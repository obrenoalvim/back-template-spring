package com.example.backtemplate.admin.dto;

import com.example.backtemplate.notes.Note;
import java.util.UUID;

public record NoteWithOwnerResponse(UUID id, String title, String ownerEmail) {

  public static NoteWithOwnerResponse from(Note note) {
    return new NoteWithOwnerResponse(note.getId(), note.getTitle(), note.getOwner().getEmail());
  }
}
