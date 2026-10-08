package com.alvexo.bookingapp.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Per-service "1 Day Capacity Change" override (MASTER_BACKEND_MVP_INTEGRATION_SPEC.md v3 §4.6) —
 * a single service's maxSlotsPerDay for one date, without touching the standing
 * {@link MechanicServiceSetting#getMaxSlotsPerDay()}. Keyed by service name (not id) since
 * that's what the workshop UI and rider UI both already display/send.
 */
@Entity
@Table(name = "mechanic_daily_service_capacities",
        uniqueConstraints = @UniqueConstraint(name = "uq_daily_override_service",
                                               columnNames = {"daily_override_id", "service_name"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MechanicDailyServiceCapacity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_override_id", nullable = false)
    private MechanicDailyOverride dailyOverride;

    @Column(name = "service_name", nullable = false, length = 100)
    private String serviceName;

    @Column(name = "capacity", nullable = false)
    private Integer capacity;
}
