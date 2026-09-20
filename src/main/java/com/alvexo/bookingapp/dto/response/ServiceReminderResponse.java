package com.alvexo.bookingapp.dto.response;

import com.alvexo.bookingapp.model.ReminderStage;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** One row of the Service Due / Second Reminder card (spec §7 #74/#75, extended per #128). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceReminderResponse {

    /** Backs POST /api/workshop/reminders/notify — null only for legacy rows with no reminder cycle yet. */
    private Long reminderCycleId;

    private Long vehicleId;
    private String vehicleRegistrationNumber;
    private String vehicleInfo;

    private Long customerId;
    private String customerName;
    private String customerPhone;

    private LocalDate lastServiceDate;
    private Integer daysSinceService;
    private ReminderStage reminderStage;

    // ── Independent reminder-cycle dates (#128 §2/§7) ─────────────────────────
    private LocalDate scheduledServiceDate;
    private LocalDateTime firstReminderPlannedAt;
    private LocalDateTime firstReminderSentAt;
    private LocalDateTime secondReminderScheduledAt;
    private LocalDateTime secondReminderSentAt;
}
