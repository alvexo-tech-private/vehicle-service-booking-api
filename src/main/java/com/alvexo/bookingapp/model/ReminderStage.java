package com.alvexo.bookingapp.model;

public enum ReminderStage {
    /** daysSinceService >= serviceDueIntervalDays */
    DUE,
    /** daysSinceService >= serviceDueIntervalDays + secondReminderIntervalDays */
    SECOND_REMINDER
}
