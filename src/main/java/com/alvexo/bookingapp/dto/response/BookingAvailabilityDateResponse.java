package com.alvexo.bookingapp.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * One day's booking availability. {@code reasonCode} is non-null exactly when
 * {@code available} is false — one of CLOSED, HOLIDAY, PAUSE, FULL,
 * SERVICE_NOT_OFFERED, VEHICLE_NOT_SUPPORTED, SETTINGS_INCOMPLETE
 * (BACKEND_REQUIREMENTS_FULL_APP_WORKSHOP_RIDER.md §3).
 */
public record BookingAvailabilityDateResponse(
        LocalDate date,
        boolean available,
        String reasonCode,
        LocalTime reportingTime,
        Integer remainingCapacity,
        java.util.List<SlotResponse> slots
) {}
