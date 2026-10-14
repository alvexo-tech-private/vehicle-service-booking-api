package com.alvexo.bookingapp.dto.response;

import java.math.BigDecimal;
import java.util.List;

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
        BigDecimal longitude,
        /** This workshop's configured vehicle makes across all categories. Empty if not configured. */
        List<String> supportedBrands,
        /** Relative to the query's ?vehicleMake= param. Null when no vehicleMake was requested. */
        Boolean isBrandSupported,
        String profileImageUrl,
        Boolean active
) {}