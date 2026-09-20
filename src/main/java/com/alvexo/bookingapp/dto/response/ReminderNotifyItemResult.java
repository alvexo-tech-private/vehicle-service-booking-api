package com.alvexo.bookingapp.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.alvexo.bookingapp.model.ReminderNotifyStage;

/**
 * {@code result} is one of: SENT, ALREADY_SENT, REMINDER_CYCLE_NOT_FOUND, NOT_ELIGIBLE,
 * SECOND_REMINDER_NOT_DUE, NOTIFICATION_PROVIDER_FAILED, RIDER_NOT_REACHABLE
 * (BACKEND_REQUIREMENTS_SERVICE_REMINDER_128.md §8).
 */
public record ReminderNotifyItemResult(
        Long reminderCycleId,
        String vehicleRegistrationNumber,
        ReminderNotifyStage stage,
        LocalDateTime firstReminderSentAt,
        LocalDate scheduledServiceDate,
        LocalDateTime secondReminderScheduledAt,
        String result
) {}
