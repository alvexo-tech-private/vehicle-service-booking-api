package com.alvexo.bookingapp.model;

/** Derived assignment status for Service Desk Pickup/Drop rows. Pickup never reaches DELIVERED. */
public enum AssignmentStatus {
    UNASSIGNED,
    ASSIGNED,
    DELIVERED
}
