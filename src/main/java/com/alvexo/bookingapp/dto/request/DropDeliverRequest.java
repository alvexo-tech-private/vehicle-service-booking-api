package com.alvexo.bookingapp.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Data;

import java.time.LocalDate;

/** PUT /api/service-desk/drops/{bookingId}/deliver (§4.2). */
@Data
public class DropDeliverRequest {

    @NotBlank(message = "deliveredBy is required")
    private String deliveredBy;

    @NotNull(message = "deliveredOn is required")
    @PastOrPresent(message = "deliveredOn cannot be in the future")
    private LocalDate deliveredOn;
}
