package com.alvexo.bookingapp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_quota_override",
        uniqueConstraints = @UniqueConstraint(
            name = "uq_daily_override_mechanic_date",
            columnNames = {"mechanic_settings_id", "override_date"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyQuotaOverride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mechanic_settings_id", nullable = false)
    private MechanicSettings mechanicSettings;

    @Column(name = "override_date", nullable = false)
    private LocalDate overrideDate;

    @Column(name = "override_max_vehicles")
    private Integer overrideMaxVehicles;

    @Column(name = "override_capacity_hours", precision = 5, scale = 2)
    private BigDecimal overrideCapacityHours;

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
