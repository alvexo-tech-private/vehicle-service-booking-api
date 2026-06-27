package com.alvexo.bookingapp.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "Walk-in booking created by mechanic for a customer. TYPE_3/4 only. Auto-confirmed with job card")
public class MechanicCreatedBookingRequest {

    @Schema(description = "Walk-in customer name", example = "Ravi Kumar",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "customerName is required")
    private String customerName;

    @Schema(description = "Customer mobile number. If registered, booking links to their account",
            example = "9876543210", nullable = true)
    private String customerMobile;

    @Schema(description = "Free-text vehicle description for walk-in customers",
            example = "Honda Activa 2022 - TN09AB1234", nullable = true)
    private String vehicleDescription;

    @Schema(description = "FK to mechanic_service_settings. Determines service duration for capacity check",
            example = "15", nullable = true)
    private Long serviceSettingId;

    @Schema(description = "Scheduled date and time for the service",
            example = "2026-07-15T09:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "scheduledDateTime is required")
    private LocalDateTime scheduledDateTime;

    @Schema(description = "Service description", example = "Oil change and brake check",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "description is required")
    private String description;

    @Schema(description = "Additional notes from the customer", example = "Walk-in customer",
            nullable = true)
    private String customerNotes;
}
