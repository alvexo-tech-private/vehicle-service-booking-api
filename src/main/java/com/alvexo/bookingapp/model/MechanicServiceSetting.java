package com.alvexo.bookingapp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "mechanic_service_settings",
        indexes = {
            @Index(name = "idx_mss_mechanic", columnList = "mechanic_id"),
            @Index(name = "idx_mss_active",   columnList = "mechanic_id, is_active")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MechanicServiceSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * FK → users.id (role = MECHANIC).
     * A mechanic can have multiple service types.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mechanic_id", nullable = false)
    private User mechanic;

    /**
     * Human-readable service name displayed to customers (e.g. "Oil Change").
     */
    @Column(name = "service_name", nullable = false, length = 100)
    private String serviceName;

    /**
     * How long this service takes in minutes.
     * Used to calculate remaining day capacity when reserveCapacity = true.
     */
    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    /**
     * Optional cap on how many times this specific service can be booked per day.
     * Null means no per-service cap (only the global day capacity applies).
     */
    @Column(name = "max_slots_per_day")
    private Integer maxSlotsPerDay;

    /**
     * Whether this service is eligible to be booked as an express slot
     * (i.e. before the mechanic's expressReportingTime).
     */
    @Column(name = "is_express_eligible", nullable = false)
    @Builder.Default
    private Boolean isExpressEligible = false;

    /**
     * Soft-delete flag. Inactive services are hidden from customers
     * but preserved for historical booking references.
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Display ordering in the UI (lower = shown first).
     */
    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
