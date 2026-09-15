package com.alvexo.bookingapp.model;

public enum BookingChannel {
    /** Vehicle owner booked through the customer-facing app. */
    ONLINE,
    /** Mechanic recorded a walk-in customer at the workshop. */
    WALK_IN,
    /** Booking originated from a rider/partner app integration. */
    RIDER_APP
}
