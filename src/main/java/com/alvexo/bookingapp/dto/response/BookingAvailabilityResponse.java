package com.alvexo.bookingapp.dto.response;

import java.util.List;

import com.alvexo.bookingapp.model.WorkshopClassification;

public record BookingAvailabilityResponse(
        Long mechanicId,
        WorkshopClassification classification,
        List<BookingAvailabilityDateResponse> dates
) {}
