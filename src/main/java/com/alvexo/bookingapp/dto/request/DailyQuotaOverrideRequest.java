package com.alvexo.bookingapp.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "Emergency one-day capacity override. Temporarily extends limits for high-demand days")
public class DailyQuotaOverrideRequest {

    @Schema(description = "The specific date to override", example = "2026-07-15",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "overrideDate is required")
    private LocalDate overrideDate;

    @Schema(description = "Override maxVehiclesPerDay for this date. null=use default",
            example = "25", nullable = true)
    @Min(value = 1, message = "overrideMaxVehicles must be at least 1")
    private Integer overrideMaxVehicles;

    @Schema(description = "Override effectiveCapacityHours for this date. null=use default",
            example = "42.00", nullable = true)
    @DecimalMin(value = "0.5", message = "overrideCapacityHours must be at least 0.5")
    private BigDecimal overrideCapacityHours;

    @Schema(description = "Reason for the override", example = "Festival rush — extending capacity",
            nullable = true)
    @Size(max = 255)
    private String reason;
}
