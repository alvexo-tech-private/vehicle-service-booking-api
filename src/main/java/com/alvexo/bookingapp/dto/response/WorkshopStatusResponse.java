package com.alvexo.bookingapp.dto.response;

import java.time.LocalDateTime;

/**
 * Workshop Current Status card (WS-STATUS-001) — onboarding/verification progress
 * with real-time booking availability and a plain-language explanation.
 */
public record WorkshopStatusResponse(
        String platformStatus,
        String trustLevel,
        String explanation,
        boolean bookingsAllowed,
        String bookingWarning,
        LocalDateTime updatedAt
) {}
