package com.example.backtemplate.notes;

import com.example.backtemplate.auth.User;
import com.example.backtemplate.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "notes")
@Getter
@Setter
@NoArgsConstructor
public class Note extends BaseEntity {

  @Column(nullable = false)
  private String title;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String content;

  @Column(name = "owner_id", nullable = false)
  private UUID ownerId;

  // Read-only navigation to the owner, alongside the raw ownerId column above.
  // insertable/updatable = false so writes still go through ownerId only —
  // this mapping exists purely so NoteRepository.findAllWithOwner() can JOIN
  // FETCH it in one query instead of the caller loading each owner by hand.
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "owner_id", insertable = false, updatable = false)
  private User owner;
}
