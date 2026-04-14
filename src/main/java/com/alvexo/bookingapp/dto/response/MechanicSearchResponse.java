package com.alvexo.bookingapp.dto.response;

import java.math.BigDecimal;

/**
 * Response DTO for mechanic search by city (and optional area).
 */
public record MechanicSearchResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String mobileNumber,
        String city,
        String area,
        String state,
        String workshopName,
        String specialization,
        Integer experienceYears,
        BigDecimal hourlyRate,
        BigDecimal rating,
        Integer totalReviews,
        Integer totalBookingsCompleted,
        String bio,
        BigDecimal latitude,
        BigDecimal longitude
) {}