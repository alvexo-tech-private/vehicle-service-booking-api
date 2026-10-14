package com.alvexo.bookingapp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Backs the canonical Booking ID series (MASTER_BACKEND_MVP_INTEGRATION_SPEC.md v3 §4.11/§6):
 * one row per workshop, holding the workshop's permanently-assigned 4-digit code (assigned once,
 * from workshop_code_seq starting at 1888), its vehicle-registration state code (e.g. "TN"), and
 * an atomically-incrementing per-workshop sequence used to build bookingNumber as
 * "{workshopCode} {stateCode} {sequence, 4-digit zero-padded}" (e.g. "1888 TN 0881").
 */
@Entity
@Table(name = "workshop_booking_sequences")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkshopBookingSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mechanic_id", nullable = false, unique = true)
    private User mechanic;

    @Column(name = "workshop_code", nullable = false, length = 10)
    private String workshopCode;

    @Column(name = "state_code", nullable = false, length = 5)
    private String stateCode;

    @Column(name = "current_sequence", nullable = false)
    @Builder.Default
    private Integer currentSequence = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
