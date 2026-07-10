package com.alvexo.bookingapp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "mechanic_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MechanicSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * One-to-one with users table (role = MECHANIC).
     * UNIQUE constraint ensures a mechanic has exactly one settings profile.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mechanic_id", nullable = false, unique = true)
    private User mechanic;

    /**
     * Maximum number of vehicles accepted per day.
     * Used as the capacity limit when reserveCapacity = false.
     */
    @Column(name = "max_vehicles_per_day", nullable = false)
    private Integer maxVehiclesPerDay;

    /**
     * If true → hour-slot mode (fullDayCapacityHours governs capacity).
     * If false → vehicle-count mode (maxVehiclesPerDay governs capacity).
     */
    @Column(name = "reserve_capacity", nullable = false)
    @Builder.Default
    private Boolean reserveCapacity = false;

    /**
     * Internal job-card generation mode behind the mechanic-facing
     * Workshop Capacity Level (1-4). AUTO -> Level 1/2, MECHANIC -> Level 3/4.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "job_card_type", nullable = false, length = 20)
    @Builder.Default
    private JobCardType jobCardType = JobCardType.AUTO;

    /**
     * Only meaningful when jobCardType = MECHANIC.
     * false -> Level 3 (Mechanic Day), true -> Level 4 (Mechanic Slot).
     */
    @Column(name = "reserve_for_slots", nullable = false)
    @Builder.Default
    private Boolean reserveForSlots = false;

    /**
     * Derived from (jobCardType, reserveCapacity, reserveForSlots) and persisted
     * for convenience — drives which Home dashboard layout the mobile app renders.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "classification", nullable = false, length = 20)
    @Builder.Default
    private WorkshopClassification classification = WorkshopClassification.TYPE_1;

    /**
     * Total working capacity in hours per day.
     * Required when reserveCapacity = true.
     * Bookings are allowed until SUM(service durations) reaches this limit.
     */
    @Column(name = "full_day_capacity_hours", precision = 5, scale = 2)
    private BigDecimal fullDayCapacityHours;

    /**
     * Job card serial prefix (e.g. "0101"). Appended with date + daily sequence
     * to generate unique job card numbers. Resets daily, Mon–Sat.
     */
    @Column(name = "job_card_serial_prefix", nullable = false, length = 10)
    private String jobCardSerialPrefix;

    /**
     * Time by which standard service vehicles must report.
     * Consolidated from the three-field (hour / minute / period) spec.
     */
    @Column(name = "service_reporting_time", nullable = false)
    private LocalTime serviceReportingTime;

    /**
     * Time by which express service vehicles must report.
     * Required when reserveCapacity = true.
     * Bookings before this time → express; on/after → standard.
     */
    @Column(name = "express_reporting_time")
    private LocalTime expressReportingTime;

    /**
     * Whether an advance payment is required at booking confirmation.
     */
    @Column(name = "advance_enabled", nullable = false)
    @Builder.Default
    private Boolean advanceEnabled = false;

    /**
     * Advance amount in INR. Required when advanceEnabled = true.
     */
    @Column(name = "advance_amount", precision = 10, scale = 2)
    private BigDecimal advanceAmount;

    /**
     * Open Booking / Auto-Allocation toggle — lets bookings be auto-assigned
     * up to autoAllocationCapacityHours worth of work per day.
     */
    @Column(name = "auto_allocation_enabled", nullable = false)
    @Builder.Default
    private Boolean autoAllocationEnabled = false;

    /**
     * Required when autoAllocationEnabled = true (min 0.5, enforced at API layer).
     */
    @Column(name = "auto_allocation_capacity_hours", precision = 5, scale = 2)
    private BigDecimal autoAllocationCapacityHours;

    /**
     * Whether job cards are issued automatically on booking confirmation
     * (true) or require an explicit mechanic action (false).
     */
    @Column(name = "auto_issue", nullable = false)
    @Builder.Default
    private Boolean autoIssue = true;

    /**
     * JSON array of service configurations offered by this mechanic.
     * Stored as JSONB — purely config, never FK-referenced.
     * Structure: [{"serviceName":"...", "durationMinutes":60, "isExpressEligible":true, ...}]
     */
    @Column(name = "preferences", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String preferences;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
