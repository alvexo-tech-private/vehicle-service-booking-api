package com.alvexo.bookingapp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Read-only (from the workshop's point of view) pickup & drop status.
 * currentStage drives the derived state of all 5 PickupDropStage entries:
 * stages before it are DONE, the current one is ACTIVE, later ones are PENDING.
 * facilityEnabled mirrors the Settings > Configuration "Pickup/Drop Facility" switch.
 */
@Entity
@Table(name = "workshop_pickup_drop_statuses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkshopPickupDropStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mechanic_id", nullable = false, unique = true)
    private User mechanic;

    @Column(name = "facility_enabled", nullable = false)
    @Builder.Default
    private Boolean facilityEnabled = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_stage", length = 20)
    private PickupDropStage currentStage;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
