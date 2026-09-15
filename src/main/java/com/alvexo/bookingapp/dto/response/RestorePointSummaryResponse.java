package com.alvexo.bookingapp.dto.response;

import java.time.LocalDateTime;

public record RestorePointSummaryResponse(
        Long id,
        String name,
        LocalDateTime savedAt
) {}
