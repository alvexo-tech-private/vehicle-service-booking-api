package com.alvexo.bookingapp.dto.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.alvexo.bookingapp.model.ServiceType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Rider booking request. Booking strategy depends on the mechanic's jobCardType (TYPE_1-4)")
public class BookingRequest {

    @Schema(description = "Mechanic user ID", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Mechanic ID is required")
    private Long mechanicId;

    @Schema(description = "Vehicle ID from user's vehicle list", example = "10",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Vehicle ID is required")
    private Long vehicleId;

    @Schema(description = "Desired service date and time. Use scheduledDateTime from SlotResponse",
            example = "2026-07-15T09:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Scheduled date time is required")
    private LocalDateTime scheduledDateTime;

    @Schema(description = "Service type category", example = "REGULAR_MAINTENANCE",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Service type is required")
    private ServiceType serviceType;

    @Schema(description = "Service description", example = "Oil change and filter replacement",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Description is required")
    private String description;

    @Schema(description = "FK to mechanic_service_settings. Required for TYPE_2/3/4 mechanics",
            example = "15", nullable = true)
    private Long serviceSettingId;

    @Schema(description = "Advance amount paid. Must match mechanic's advanceAmount when advanceEnabled=true",
            example = "100.00", nullable = true)
    private BigDecimal advancePaid;

    @Schema(description = "Estimated cost in INR", example = "500.00", nullable = true)
    private BigDecimal estimatedCost;

    @Schema(description = "Estimated duration in minutes (overridden by serviceSetting if provided)",
            example = "60", nullable = true)
    private Integer estimatedDurationMinutes;

    @Schema(description = "Customer notes", example = "Prefer morning slots", nullable = true)
    private String customerNotes;
}
