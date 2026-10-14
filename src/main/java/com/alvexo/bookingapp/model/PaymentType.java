package com.alvexo.bookingapp.model;

public enum PaymentType {
    BOOKING_PAYMENT,
    SUBSCRIPTION_PAYMENT,
    REFERRAL_BONUS,
    /** Rider-side platform fee (§6) — distinct ledger leg, never included in mechanic earnings/settlement. */
    PLATFORM_FEE
}
