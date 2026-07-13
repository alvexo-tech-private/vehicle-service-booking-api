package com.alvexo.bookingapp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Generic uploaded-file record. The binary content lives on disk under the
 * configured upload root; this row tracks metadata and ownership so access can
 * be authorised per-request. Content is streamed back via FileController.
 */
@Entity
@Table(name = "stored_files", indexes = @Index(name = "idx_stored_files_owner", columnList = "owner_id"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoredFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The user (workshop) this file belongs to. Used to authorise downloads.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    /**
     * Path relative to the configured upload root (never the raw filename —
     * stored under a generated UUID to avoid collisions/path traversal).
     */
    @Column(name = "storage_path", nullable = false, length = 500)
    private String storagePath;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
