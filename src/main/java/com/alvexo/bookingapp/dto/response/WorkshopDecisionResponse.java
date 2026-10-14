package com.alvexo.bookingapp.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.alvexo.bookingapp.model.BookingStatus;

public record WorkshopDecisionResponse(
        Long bookingId,
        BookingStatus status,
        /** Null when rejected — the rider has nothing left to confirm. */
        LocalDateTime confirmationExpiresAt,
        BigDecimal requiredAdvance
) {}
