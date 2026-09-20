package com.alvexo.bookingapp.model;

/** Lifecycle of one {@link ReminderCycle} (BACKEND_REQUIREMENTS_SERVICE_REMINDER_128.md). */
public enum ReminderCycleStatus {
    PLANNED,
    FIRST_SENT,
    SECOND_SENT,
    /** Superseded by a newer cycle for the same (mechanic, vehicle, customer) — service completed again. */
    CLOSED
}
