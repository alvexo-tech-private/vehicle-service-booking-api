package com.alvexo.bookingapp.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * PUT /api/bookings/{id}/cancel body (RIDER_BOOKING_TO_WORKSHOP.md §5) —
 * the rider's own cancellation of a booking they created.
 */
@Data
public class BookingCancelRequest {

    @NotBlank(message = "reason is required")
    @Size(max = 500, message = "reason must be at most 500 characters")
    private String reason;
}
