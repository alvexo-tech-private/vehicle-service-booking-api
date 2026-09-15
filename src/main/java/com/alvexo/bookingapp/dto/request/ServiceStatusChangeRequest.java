package com.alvexo.bookingapp.dto.request;

import com.alvexo.bookingapp.model.ServiceWorkspaceStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * PUT /api/service-desk/bookings/{bookingId}/status (§1.4). Only PENDING and
 * COMPLETED are accepted here — ARRIVED requires a job card number (use
 * /arrive) and CANCELLED requires a message (use /cancel).
 */
@Data
public class ServiceStatusChangeRequest {

    @NotNull(message = "status is required")
    private ServiceWorkspaceStatus status;
}
