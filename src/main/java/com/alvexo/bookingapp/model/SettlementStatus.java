package com.alvexo.bookingapp.model;

/** Settlement lifecycle (WORKSHOP_FINANCE_API_SPEC.md §2): Pending -> Processing -> Paid. */
public enum SettlementStatus {
    PENDING,
    PROCESSING,
    PAID
}
