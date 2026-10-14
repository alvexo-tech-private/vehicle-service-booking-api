package com.alvexo.bookingapp.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/** PUT /api/service-desk/service-week/{id}/reschedule (§2.4). */
@Data
public class WeekRescheduleRequest {

    @NotNull(message = "toDate is required")
    @Future(message = "toDate must be a future date")
    private LocalDate toDate;
}
