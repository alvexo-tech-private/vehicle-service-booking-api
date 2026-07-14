package com.alvexo.bookingapp.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** PUT /api/service-desk/bookings/{bookingId}/arrive (§1.5). */
@Data
public class ServiceArriveRequest {

    @NotBlank(message = "jobCardNumber is required")
    @Size(max = 30, message = "jobCardNumber must be at most 30 characters")
    private String jobCardNumber;
}
