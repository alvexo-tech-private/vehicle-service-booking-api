package com.alvexo.bookingapp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_user_id", nullable = false)
    private User vehicleUser;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mechanic_id", nullable = false)
    private User mechanic;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    /**
     * FK → mechanic_service_settings.id
     * Nullable for backward compatibility with pre-existing bookings.
     * When present, used to derive duration for capacity checks and
     * enforce per-service daily caps.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_setting_id")
    private MechanicServiceSetting serviceSetting;

    @Column(name = "booking_number", nullable = false, unique = true)
    private String bookingNumber;

    /**
     * Generated on booking confirmation:
     * {jobCardSerialPrefix}{YYMMDD}{dailySequence}
     * e.g. "010125040801"
     */
    @Column(name = "job_card_number", unique = true)
    private String jobCardNumber;

    @Column(name = "scheduled_date_time", nullable = false)
    private LocalDateTime scheduledDateTime;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    /**
     * Service Desk sub-state while status = IN_PROGRESS (Arrived vs Pending).
     * Null before arrival and once status leaves IN_PROGRESS.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "service_stage", length = 20)
    private ServiceDeskStage serviceStage;

    /**
     * STANDARD — vehicle reports at or after mechanic's serviceReportingTime.
     * EXPRESS  — vehicle reports before mechanic's expressReportingTime
     *            (only when mechanic's reserveCapacity = true).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "booking_type", nullable = false)
    @Builder.Default
    private BookingType bookingType = BookingType.STANDARD;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false)
    private ServiceType serviceType;

    /** ONLINE (customer app) / WALK_IN (recorded by mechanic) / RIDER_APP. */
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    @Builder.Default
    private BookingChannel channel = BookingChannel.ONLINE;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;
    
    @Column(name = "estimated_cost", precision = 10, scale = 2)
    private BigDecimal estimatedCost;
    
    @Column(name = "actual_cost", precision = 10, scale = 2)
    private BigDecimal actualCost;
    
    @Column(name = "estimated_duration_minutes")
    private Integer estimatedDurationMinutes;
    
    @Column(name = "actual_duration_minutes")
    private Integer actualDurationMinutes;

    /**
     * Advance amount collected at booking time.
     * Must equal mechanic_settings.advance_amount before status → CONFIRMED
     * when the mechanic has advanceEnabled = true.
     */
    @Column(name = "advance_paid", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal advancePaid = BigDecimal.ZERO;

    @Column(name = "mechanic_notes", columnDefinition = "TEXT")
    private String mechanicNotes;
    
    @Column(name = "customer_notes", columnDefinition = "TEXT")
    private String customerNotes;
    
    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    /**
     * Customer-facing cancellation message shown by the Service Desk Cancel
     * action (spec §1.7/§2.5) — distinct from the internal cancellationReason.
     */
    @Column(name = "cancellation_message", columnDefinition = "TEXT")
    private String cancellationMessage;

    /**
     * ₹30 Service Reliability Adjustment (spec §1.7, BR-22/30) — set when this
     * booking was cancelled after the mechanic's rescheduleCutoffTime. Feeds
     * the Earnings tab's cancellation-deduction entries. Null = not applicable.
     */
    @Column(name = "reliability_adjustment_amount", precision = 10, scale = 2)
    private BigDecimal reliabilityAdjustmentAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_by")
    private User cancelledBy;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    // ── Service Desk fields (pickup / drop / carry-over) ────────────────────

    @Column(name = "pickup_required", nullable = false)
    @Builder.Default
    private Boolean pickupRequired = false;

    @Column(name = "drop_required", nullable = false)
    @Builder.Default
    private Boolean dropRequired = false;

    @Column(name = "pickup_address", columnDefinition = "TEXT")
    private String pickupAddress;

    @Column(name = "delivery_address", columnDefinition = "TEXT")
    private String deliveryAddress;

    /** Junior mechanic assigned to pick the vehicle up — distinct from the servicing mechanic. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pickup_mechanic_id")
    private MechanicMasterEntry pickupMechanic;

    /** Junior mechanic assigned to drop the vehicle back — distinct from the servicing mechanic. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "drop_mechanic_id")
    private MechanicMasterEntry dropMechanic;

    @Column(name = "delivered_by", length = 100)
    private String deliveredBy;

    @Column(name = "delivered_on")
    private LocalDate deliveredOn;

    /** True once this booking is carried over from a previous, unfinished service day. */
    @Column(name = "is_carry_over", nullable = false)
    @Builder.Default
    private Boolean isCarryOver = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
