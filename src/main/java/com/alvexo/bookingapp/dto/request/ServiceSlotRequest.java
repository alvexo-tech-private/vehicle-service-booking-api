package com.alvexo.bookingapp.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalTime;

@Data
public class ServiceSlotRequest {

    @NotNull(message = "slotNumber is required")
    @Min(value = 1, message = "slotNumber must be 1 or 2")
    @Max(value = 2, message = "slotNumber must be 1 or 2")
    private Integer slotNumber;

    /** Required when enabled = true; validated in the service layer. */
    private LocalTime slotTime;

    @NotNull(message = "repairQty is required")
    @Min(value = 1, message = "repairQty must be at least 1")
    private Integer repairQty;

    private Boolean enabled = true;
}
