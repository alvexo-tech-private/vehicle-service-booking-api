package com.alvexo.bookingapp.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One service-reminder cycle for a (mechanic, vehicle, customer) triple
 * (BACKEND_REQUIREMENTS_SERVICE_REMINDER_128.md). Each date is tracked independently:
 * {@code scheduledServiceDate} never changes once set, and {@code secondReminderScheduledAt}
 * is always derived from the ACTUAL {@code firstReminderSentAt} — never from the due date,
 * the planned time, or a fixed interval off last service.
 */
@Entity
@Table(name = "reminder_cycles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReminderCycle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mechanic_id", nullable = false)
    private User mechanic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @Column(name = "last_service_date", nullable = false)
    private LocalDate lastServiceDate;

    /** Immutable for this cycle — reminder delivery never recalculates this. */
    @Column(name = "scheduled_service_date", nullable = false)
    private LocalDate scheduledServiceDate;

    @Column(name = "first_reminder_planned_at")
    private LocalDateTime firstReminderPlannedAt;

    /** Set only after the first reminder is actually (successfully) sent. */
    @Column(name = "first_reminder_sent_at")
    private LocalDateTime firstReminderSentAt;

    /** = firstReminderSentAt + the mechanic's secondReminderIntervalDays, computed at send time. */
    @Column(name = "second_reminder_scheduled_at")
    private LocalDateTime secondReminderScheduledAt;

    @Column(name = "second_reminder_sent_at")
    private LocalDateTime secondReminderSentAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ReminderCycleStatus status = ReminderCycleStatus.PLANNED;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
