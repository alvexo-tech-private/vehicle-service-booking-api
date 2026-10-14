package com.alvexo.bookingapp.model;

/** Settlement lifecycle (WORKSHOP_FINANCE_API_SPEC.md §2): Pending -> Processing -> Paid. */
public enum SettlementStatus {
    PENDING,
    PROCESSING,
    PAID,
    /** No advance was collected that day — never appears as a misleading zero-value Pending row. */
    NO_SETTLEMENT_REQUIRED
}
