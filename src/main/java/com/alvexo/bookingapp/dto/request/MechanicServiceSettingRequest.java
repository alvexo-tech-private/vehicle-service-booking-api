package com.alvexo.bookingapp.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MechanicServiceSettingRequest {

    @NotBlank(message = "serviceName is required")
    @Size(max = 100)
    private String serviceName;

    @NotNull(message = "durationMinutes is required")
    @Min(value = 1, message = "durationMinutes must be at least 1")
    private Integer durationMinutes;

    @Min(value = 1, message = "No of Vehicle must be at least 1")
    private Integer noOfVehicle;

    @Min(value = 1, message = "maxSlotsPerDay must be at least 1")
    private Integer maxSlotsPerDay;

    private Boolean isExpressEligible = false;

    private Boolean isActive = true;

    private Integer displayOrder = 0;
}
