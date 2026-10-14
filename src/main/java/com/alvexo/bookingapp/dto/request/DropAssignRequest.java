package com.alvexo.bookingapp.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** PUT /api/service-desk/drops/{bookingId}/assign (§4.2). */
@Data
public class DropAssignRequest {

    @NotNull(message = "mechanicId is required")
    private Long mechanicId;
}
