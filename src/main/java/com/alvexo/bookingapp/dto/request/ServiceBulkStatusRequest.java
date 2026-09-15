package com.alvexo.bookingapp.dto.request;

import com.alvexo.bookingapp.model.ServiceWorkspaceStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/** POST /api/service-desk/bookings/bulk-status (§1.8). */
@Data
public class ServiceBulkStatusRequest {

    @NotEmpty(message = "bookingIds must not be empty")
    private List<@NotNull String> bookingIds;

    @NotNull(message = "status is required")
    private ServiceWorkspaceStatus status;
}
