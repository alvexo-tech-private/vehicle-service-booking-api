package com.alvexo.bookingapp.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class MechanicDailyOverrideRequest {

    @NotNull(message = "date is required")
    private LocalDate date;

    /** Null = no override for this field; falls back to standing MechanicSettings value. */
    @Min(value = 1, message = "maxVehiclesPerDayOverride must be at least 1")
    private Integer maxVehiclesPerDayOverride;

    @DecimalMin(value = "0.5", message = "fullDayCapacityHoursOverride must be at least 0.5")
    private BigDecimal fullDayCapacityHoursOverride;

    private Boolean advanceEnabledOverride;

    @DecimalMin(value = "0.01", message = "advanceAmountOverride must be greater than 0")
    private BigDecimal advanceAmountOverride;
}
