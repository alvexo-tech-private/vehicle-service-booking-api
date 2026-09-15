package com.alvexo.bookingapp.model;

/**
 * Service Desk "Today" tab status — a finer-grained view over BookingStatus.
 * ARRIVED and PENDING are both sub-states of BookingStatus.IN_PROGRESS,
 * distinguished by Booking.serviceStage. See ServiceDeskMapper for the mapping.
 */
public enum ServiceWorkspaceStatus {
    SCHEDULED,
    ARRIVED,
    PENDING,
    COMPLETED,
    CANCELLED
}
