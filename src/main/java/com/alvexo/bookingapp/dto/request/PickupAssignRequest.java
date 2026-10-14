package com.alvexo.bookingapp.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** PUT /api/service-desk/pickups/{bookingId}/assign (§3.2). */
@Data
public class PickupAssignRequest {

    @NotNull(message = "mechanicId is required")
    private Long mechanicId;
}
