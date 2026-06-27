package com.alvexo.bookingapp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "mechanic_technician_capacity",
        indexes = {
            @Index(name = "idx_tech_capacity_settings", columnList = "mechanic_settings_id"),
            @Index(name = "idx_tech_capacity_active", columnList = "mechanic_settings_id, is_active")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MechanicTechnicianCapacity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mechanic_settings_id", nullable = false)
    private MechanicSettings mechanicSettings;

    @Column(name = "technician_name", nullable = false, length = 100)
    private String technicianName;

    @Column(name = "reserved_hours", nullable = false, precision = 5, scale = 2)
    private BigDecimal reservedHours;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
