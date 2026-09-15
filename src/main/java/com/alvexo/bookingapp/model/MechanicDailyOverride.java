package com.alvexo.bookingapp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Per-date override of capacity/advance settings (spec §7 Home "One-Day
 * Capacity & Advance Change"). Any field left null falls back to the
 * mechanic's standing MechanicSettings value for that date.
 */
@Entity
@Table(name = "mechanic_daily_overrides",
        uniqueConstraints = @UniqueConstraint(name = "uq_mechanic_daily_overrides_mechanic_date",
                                               columnNames = {"mechanic_id", "date"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MechanicDailyOverride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mechanic_id", nullable = false)
    private User mechanic;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "max_vehicles_per_day_override")
    private Integer maxVehiclesPerDayOverride;

    @Column(name = "full_day_capacity_hours_override", precision = 5, scale = 2)
    private BigDecimal fullDayCapacityHoursOverride;

    @Column(name = "advance_enabled_override")
    private Boolean advanceEnabledOverride;

    @Column(name = "advance_amount_override", precision = 10, scale = 2)
    private BigDecimal advanceAmountOverride;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
