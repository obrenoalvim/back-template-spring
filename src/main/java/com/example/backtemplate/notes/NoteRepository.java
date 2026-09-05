package com.example.backtemplate.notes;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface NoteRepository extends JpaRepository<Note, UUID> {
  List<Note> findAllByOwnerId(UUID ownerId);

  Optional<Note> findByIdAndOwnerId(UUID id, UUID ownerId);

  // JOIN FETCH loads owner in the same SQL query — without it, reading
  // note.getOwner() for each row in a loop would issue one query per note.
  @Query("SELECT n FROM Note n JOIN FETCH n.owner")
  List<Note> findAllWithOwner();
}
