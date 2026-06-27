package com.alvexo.bookingapp.dto.request;

import com.alvexo.bookingapp.model.ServiceCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalTime;

@Data
@Schema(description = "Time-windowed service slot for TYPE_4 mechanics. Each slot is restricted to a specific service category")
public class MechanicServiceSlotRequest {

    @Schema(description = "Slot sequence number (1, 2, 3...)", example = "1",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "slotNumber is required")
    @Min(1)
    private Integer slotNumber;

    @Schema(description = "Slot start time", example = "11:00",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "startTime is required")
    private LocalTime startTime;

    @Schema(description = "Slot end time", example = "13:00",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "endTime is required")
    private LocalTime endTime;

    @Schema(description = "Service category this slot is exclusively reserved for. "
            + "Riders booking other categories will not see this slot",
            example = "REPAIR", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "restrictedCategory is required")
    private ServiceCategory restrictedCategory;

    @Schema(description = "Maximum vehicles for this slot per day", example = "5",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "maxVehicleQty is required")
    @Min(value = 1, message = "maxVehicleQty must be at least 1")
    private Integer maxVehicleQty;

    @Schema(description = "Auto-confirmed portion of maxVehicleQty. Remaining goes to manual review queue. Must be ≤ maxVehicleQty",
            example = "3")
    @Min(value = 0, message = "autoAllocationQty must be at least 0")
    private Integer autoAllocationQty = 0;

    @Schema(description = "Comma-separated weekdays this slot is active. null=all days",
            example = "MON,WED,FRI", nullable = true)
    private String applicableDays;

    @Schema(description = "Whether this slot is visible to riders", example = "true")
    private Boolean isEnabled = true;
}
