package com.alvexo.bookingapp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Cross-cutting workshop state read by "Current Status" (Menu 1) and
 * "Operational Trust" (Menu 9). Everything else (owner info, images, etc.)
 * lives in its own table; this just tracks the overall status flags.
 */
@Entity
@Table(name = "workshop_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkshopProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mechanic_id", nullable = false, unique = true)
    private User mechanic;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private WorkshopStatus status = WorkshopStatus.NEW_TO_APP;

    @Column(name = "suspended", nullable = false)
    @Builder.Default
    private Boolean suspended = false;

    @Column(name = "suspension_reason", columnDefinition = "TEXT")
    private String suspensionReason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
