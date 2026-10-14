package com.alvexo.bookingapp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Global legal document catalogue (Terms, Privacy, Disclaimer) — not per-workshop.
 * Seeded via Liquibase; version bumps happen out-of-band when legal content changes.
 */
@Entity
@Table(name = "legal_documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LegalDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, unique = true, length = 20)
    private LegalDocumentType documentType;

    @Column(name = "version", nullable = false, length = 20)
    private String version;

    @Column(name = "url", nullable = false, length = 500)
    private String url;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
