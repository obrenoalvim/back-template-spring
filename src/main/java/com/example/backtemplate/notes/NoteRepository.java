package com.example.backtemplate.notes;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoteRepository extends JpaRepository<Note, UUID> {
    List<Note> findAllByOwnerId(UUID ownerId);

    Optional<Note> findByIdAndOwnerId(UUID id, UUID ownerId);
}
