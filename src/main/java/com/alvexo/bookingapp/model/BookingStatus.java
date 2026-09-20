package com.alvexo.bookingapp.model;

public enum BookingStatus {
    PENDING,
    CONFIRMED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
    REJECTED,

    // ── Today Approval two-stage flow (BACKEND_REQUIREMENTS_FULL_APP_WORKSHOP_RIDER.md §5) ──
    /** Rider submitted a Today Approval request; workshop hasn't decided yet. */
    REQUESTED,
    /** Workshop accepted but the rider still owes advance before the slot is reserved. */
    PENDING_PAYMENT,
    /** Rider confirmed (and paid, if required) — capacity is reserved. Terminal success state for this flow. */
    SCHEDULED,
    /** Rider didn't confirm within confirmationExpiresAt — the held capacity was released. */
    EXPIRED
}
