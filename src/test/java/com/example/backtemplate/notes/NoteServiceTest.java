package com.example.backtemplate.notes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.example.backtemplate.common.ApiException;
import com.example.backtemplate.notes.dto.NoteRequest;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NoteServiceTest {

    @Mock private NoteRepository noteRepository;

    private NoteService noteService;

    @Test
    void getThrowsNotFoundWhenNoteMissingOrNotOwned() {
        noteService = new NoteService(noteRepository);
        UUID ownerId = UUID.randomUUID();
        UUID noteId = UUID.randomUUID();
        when(noteRepository.findByIdAndOwnerId(noteId, ownerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteService.get(ownerId, noteId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Note not found");
    }

    @Test
    void createSavesNoteWithOwnerId() {
        noteService = new NoteService(noteRepository);
        UUID ownerId = UUID.randomUUID();
        NoteRequest req = new NoteRequest("Title", "Content");
        when(noteRepository.save(org.mockito.ArgumentMatchers.any(Note.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        var response = noteService.create(ownerId, req);

        assertThat(response.title()).isEqualTo("Title");
    }
}
