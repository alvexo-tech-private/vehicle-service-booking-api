package com.alvexo.bookingapp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * The "Configuration Settings" section of Workshop Settings — separate from
 * MechanicSettings because these fields don't affect capacity/booking logic
 * directly and are edited independently. One row per mechanic.
 */
@Entity
@Table(name = "mechanic_configuration_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MechanicConfigurationSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mechanic_id", nullable = false, unique = true)
    private User mechanic;

    // ── Job Card Number ────────────────────────────────────────────────────
    @Column(name = "job_card_number_starting_sequence", nullable = false, length = 20)
    @Builder.Default
    private String jobCardNumberStartingSequence = "0001";

    @Enumerated(EnumType.STRING)
    @Column(name = "job_card_number_format", nullable = false, length = 20)
    @Builder.Default
    private JobCardNumberFormat jobCardNumberFormat = JobCardNumberFormat.NUMERIC;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_card_number_reset_frequency", nullable = false, length = 20)
    @Builder.Default
    private ResetFrequency jobCardNumberResetFrequency = ResetFrequency.WEEKLY;

    @Column(name = "job_card_number_prefix", length = 4)
    private String jobCardNumberPrefix;

    @Column(name = "job_card_number_suffix", length = 4)
    private String jobCardNumberSuffix;

    // ── Reschedule policy ───────────────────────────────────────────────────
    @Column(name = "reschedule_limit", nullable = false)
    @Builder.Default
    private Integer rescheduleLimit = 2;

    @Column(name = "reschedule_cutoff_time", nullable = false)
    @Builder.Default
    private LocalTime rescheduleCutoffTime = LocalTime.of(10, 0);

    // ── Toggles ─────────────────────────────────────────────────────────────
    @Column(name = "pickup_drop_facility_enabled", nullable = false)
    @Builder.Default
    private Boolean pickupDropFacilityEnabled = false;

    @Column(name = "auto_confirm_other_state", nullable = false)
    @Builder.Default
    private Boolean autoConfirmOtherState = true;

    @Column(name = "repairs_require_advance", nullable = false)
    @Builder.Default
    private Boolean repairsRequireAdvance = false;

    // ── Service reminders (#74) ─────────────────────────────────────────────
    @Column(name = "service_due_interval_days", nullable = false)
    @Builder.Default
    private Integer serviceDueIntervalDays = 90;

    @Column(name = "second_reminder_interval_days", nullable = false)
    @Builder.Default
    private Integer secondReminderIntervalDays = 15;

    /** How many times the reminder intervals have been changed this calendar year (capped at 2). */
    @Column(name = "reminder_changes_this_year", nullable = false)
    @Builder.Default
    private Integer reminderChangesThisYear = 0;

    /** Calendar year the above counter applies to — resets when the year rolls over. */
    @Column(name = "reminder_changes_year")
    private Integer reminderChangesYear;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
