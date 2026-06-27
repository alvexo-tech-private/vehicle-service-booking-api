package com.alvexo.bookingapp.dto.request;

import com.alvexo.bookingapp.model.ServiceCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@Schema(description = "Service offered by a mechanic — defines duration, caps, and category for capacity planning")
public class MechanicServiceSettingRequest {

    @Schema(description = "Service category for slot routing (TYPE_4) and capacity planning",
            example = "GENERAL", allowableValues = {"GENERAL", "EXPRESS", "REPAIR", "COMPLEX"})
    private ServiceCategory category = ServiceCategory.GENERAL;

    @Schema(description = "Display name shown to customers", example = "Oil Change",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "serviceName is required")
    @Size(max = 100)
    private String serviceName;

    @Schema(description = "Service duration in minutes. Used for hour-slot capacity calculation",
            example = "60", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "durationMinutes is required")
    @Min(value = 1, message = "durationMinutes must be at least 1")
    private Integer durationMinutes;

    @Schema(description = "Per-service daily cap. null=no cap (only global capacity applies)",
            example = "5", nullable = true)
    @Min(value = 1, message = "maxSlotsPerDay must be at least 1")
    private Integer maxSlotsPerDay;

    @Schema(description = "Whether this service can be booked as EXPRESS (before expressReportingTime)",
            example = "false")
    private Boolean isExpressEligible = false;

    @Schema(description = "Soft-delete flag. Inactive services are hidden from customers", example = "true")
    private Boolean isActive = true;

    @Schema(description = "UI display ordering (lower = shown first)", example = "0")
    private Integer displayOrder = 0;
}
