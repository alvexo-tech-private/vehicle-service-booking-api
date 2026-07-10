package com.alvexo.bookingapp.dto.request;

import com.alvexo.bookingapp.model.ServiceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Mechanic-recorded walk-in booking. Unlike BookingRequest, mechanicId is
 * implicit (the authenticated mechanic) and scheduledDateTime defaults to now.
 */
@Data
public class WalkInBookingRequest {

    @NotNull(message = "Vehicle user ID is required")
    private Long vehicleUserId;

    @NotNull(message = "Vehicle ID is required")
    private Long vehicleId;

    /** Defaults to now if omitted — walk-ins are typically recorded on arrival. */
    private LocalDateTime scheduledDateTime;

    @NotNull(message = "Service type is required")
    private ServiceType serviceType;

    @NotBlank(message = "Description is required")
    private String description;

    private Long serviceSettingId;

    private BigDecimal advancePaid;
    private BigDecimal estimatedCost;
    private Integer estimatedDurationMinutes;
    private String customerNotes;
}
