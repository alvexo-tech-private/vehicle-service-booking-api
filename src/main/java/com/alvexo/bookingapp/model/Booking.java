package com.alvexo.bookingapp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
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
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_by")
    private User cancelledBy;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
