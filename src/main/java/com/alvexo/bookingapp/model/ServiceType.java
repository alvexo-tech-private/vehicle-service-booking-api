package com.alvexo.bookingapp.model;

public enum ServiceType {
    GENERAL_REPAIR,
    OIL_CHANGE,
    BRAKE_SERVICE,
    TIRE_SERVICE,
    ENGINE_DIAGNOSTIC,
    TRANSMISSION_SERVICE,
    ELECTRICAL_REPAIR,
    AC_SERVICE,
    INSPECTION,
    MAINTENANCE,
    BODYWORK,
    OTHER,

    /**
     * Coarse rider-app categories (RIDER_BOOKING_TO_WORKSHOP.md §3.1) — the
     * rider "Book Now" flow only offers General/Express/Repair, not the
     * granular categories above. EXPRESS_SERVICE is a category the rider
     * picks, distinct from the server-derived Booking.bookingType
     * (STANDARD/EXPRESS), which is about reporting-time eligibility, not
     * what the customer selected.
     */
    GENERAL_SERVICE,
    EXPRESS_SERVICE
}
