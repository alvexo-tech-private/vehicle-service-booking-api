package com.alvexo.bookingapp.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Workshop technician with reserved working hours. Effective capacity = SUM of active technicians' hours")
public class MechanicTechnicianCapacityRequest {

    @Schema(description = "Technician display name", example = "John",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "technicianName is required")
    @Size(max = 100)
    private String technicianName;

    @Schema(description = "Working hours per day for this technician", example = "9.00",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "reservedHours is required")
    @DecimalMin(value = "0.5", message = "reservedHours must be at least 0.5")
    private BigDecimal reservedHours;

    @Schema(description = "Whether technician is active (present today). false=absent, reduces effective capacity",
            example = "true")
    private Boolean isActive = true;
}
