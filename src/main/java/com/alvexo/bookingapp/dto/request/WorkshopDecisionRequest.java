package com.alvexo.bookingapp.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WorkshopDecisionRequest {

    @NotNull(message = "accept is required")
    private Boolean accept;
}
