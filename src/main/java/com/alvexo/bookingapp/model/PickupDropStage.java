package com.alvexo.bookingapp.model;

/**
 * The 5 fixed stages of a pickup & drop cycle, shown read-only on the workshop profile.
 */
public enum PickupDropStage {
    REQUESTED,
    DRIVER_ASSIGNED,
    PICKED_UP,
    IN_SERVICE,
    RETURNED
}
