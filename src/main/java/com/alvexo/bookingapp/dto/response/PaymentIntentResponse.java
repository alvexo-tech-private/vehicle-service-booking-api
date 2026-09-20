package com.alvexo.bookingapp.dto.response;

import java.math.BigDecimal;

/**
 * Rider payment contract (BACKEND_REQUIREMENTS_FULL_APP_WORKSHOP_RIDER.md §6) — the workshop
 * advance and the rider platform fee are always broken out as distinct amounts.
 */
public record PaymentIntentResponse(
        Long bookingId,
        BigDecimal advanceAmount,
        BigDecimal platformFee,
        BigDecimal totalPayable,
        String currency,
        String paymentIntentId
) {}
