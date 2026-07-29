package com.alvexo.bookingapp.model;

/**
 * Persisted sub-state of a Booking while its BookingStatus = IN_PROGRESS,
 * driving the Service Desk Arrived/Pending split (§1 of the Service Desk
 * API spec). Null while the booking hasn't arrived yet, or once it leaves
 * IN_PROGRESS (COMPLETED/CANCELLED).
 */
public enum ServiceDeskStage {
    ARRIVED,
    PENDING
}
