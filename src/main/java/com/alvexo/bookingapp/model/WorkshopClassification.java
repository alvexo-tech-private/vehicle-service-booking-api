package com.alvexo.bookingapp.model;

/**
 * Derived from (jobCardType, reserveCapacity, reserveForSlots) — mirrors the
 * mechanic-facing "Workshop Capacity Level" 1-4. See WORKSHOP_SETTINGS_HOME_SPEC.md.
 *
 * Level 1 -> TYPE_1 (AUTO,     reserveCapacity=false)
 * Level 2 -> TYPE_2 (AUTO,     reserveCapacity=true)
 * Level 3 -> TYPE_3 (MECHANIC, reserveForSlots=false)
 * Level 4 -> TYPE_4 (MECHANIC, reserveForSlots=true)
 */
public enum WorkshopClassification {
    TYPE_1,
    TYPE_2,
    TYPE_3,
    TYPE_4
}
