package com.example.backtemplate.notes;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.backtemplate.AbstractIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class NoteRepositoryTest extends AbstractIntegrationTest {

  @Autowired private NoteRepository noteRepository;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void savesAndFindsNotesByOwner() {
    UUID ownerId = insertUser("owner@example.com");

    Note note = new Note();
    note.setTitle("First note");
    note.setContent("Body");
    note.setOwnerId(ownerId);
    noteRepository.save(note);

    var found = noteRepository.findAllByOwnerId(ownerId);

    assertThat(found).hasSize(1);
    assertThat(found.get(0).getTitle()).isEqualTo("First note");
  }

  private UUID insertUser(String email) {
    return jdbcTemplate.queryForObject(
        "INSERT INTO users (email) VALUES (?) RETURNING id", UUID.class, email);
  }
}
