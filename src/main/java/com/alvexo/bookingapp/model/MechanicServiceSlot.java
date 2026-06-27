package com.alvexo.bookingapp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "mechanic_service_slots",
        indexes = {
            @Index(name = "idx_service_slots_settings", columnList = "mechanic_settings_id"),
            @Index(name = "idx_service_slots_category", columnList = "mechanic_settings_id, restricted_category")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MechanicServiceSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mechanic_settings_id", nullable = false)
    private MechanicSettings mechanicSettings;

    @Column(name = "slot_number", nullable = false)
    private Integer slotNumber;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "restricted_category", nullable = false)
    private ServiceCategory restrictedCategory;

    @Column(name = "max_vehicle_qty", nullable = false)
    private Integer maxVehicleQty;

    @Column(name = "auto_allocation_qty", nullable = false)
    @Builder.Default
    private Integer autoAllocationQty = 0;

    @Column(name = "applicable_days", length = 27)
    private String applicableDays;

    @Column(name = "is_enabled")
    @Builder.Default
    private Boolean isEnabled = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Integer getManualReviewQty() {
        return maxVehicleQty - autoAllocationQty;
    }

    public boolean isApplicableOn(java.time.DayOfWeek day) {
        if (applicableDays == null || applicableDays.isBlank()) return true;
        String dayStr = day.name().substring(0, 3);
        return applicableDays.contains(dayStr);
    }
}
