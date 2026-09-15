package com.alvexo.bookingapp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "workshop_legal_consents", indexes = @Index(name = "idx_wlc_mechanic", columnList = "mechanic_id"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkshopLegalConsent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mechanic_id", nullable = false)
    private User mechanic;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 20)
    private LegalDocumentType documentType;

    @Column(name = "version", nullable = false, length = 20)
    private String version;

    @CreationTimestamp
    @Column(name = "accepted_at", updatable = false)
    private LocalDateTime acceptedAt;
}
