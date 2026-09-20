package com.alvexo.bookingapp.dto.response;

import java.math.BigDecimal;

import com.alvexo.bookingapp.model.BookingStatus;

public record ConfirmRequestResponse(
        Long bookingId,
        BookingStatus status,
        /** True when {@code requiredAdvance} still needs to be paid via /payment-intent before this can schedule. */
        boolean paymentRequired,
        BigDecimal requiredAdvance
) {}
