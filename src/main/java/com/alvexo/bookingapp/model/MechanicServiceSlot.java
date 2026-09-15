package com.alvexo.bookingapp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Slot 1 / Slot 2 configuration for Workshop Capacity Level 4 (Mechanic Slot).
 * One row per (mechanic, slotNumber).
 */
@Entity
@Table(name = "mechanic_service_slots",
        uniqueConstraints = @UniqueConstraint(name = "uq_mechanic_service_slots_mechanic_slot",
                                               columnNames = {"mechanic_id", "slot_number"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MechanicServiceSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mechanic_id", nullable = false)
    private User mechanic;

    /** 1 or 2 — enforced at the API layer. */
    @Column(name = "slot_number", nullable = false)
    private Integer slotNumber;

    @Column(name = "slot_time", nullable = false)
    private LocalTime slotTime;

    /** Planned vehicle capacity for this slot ("Veh. Qty"). */
    @Column(name = "repair_qty", nullable = false)
    private Integer repairQty;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
