package com.alvexo.bookingapp.dto.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.alvexo.bookingapp.model.ServiceType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BookingRequest {
    @NotNull(message = "Mechanic ID is required")
    private Long mechanicId;
    
    @NotNull(message = "Vehicle ID is required")
    private Long vehicleId;
    
    @NotNull(message = "Scheduled date time is required")
    private LocalDateTime scheduledDateTime;
    
    @NotNull(message = "Service type is required")
    private ServiceType serviceType;
    
    @NotBlank(message = "Description is required")
    private String description;

    /**
     * FK → mechanic_service_settings.id
     * Required when the mechanic operates in hour-slot mode (reserveCapacity = true).
     * Optional in vehicle-count mode — can still be provided for reference.
     */
    private Long serviceSettingId;

    /**
     * Advance amount paid by the customer at booking time.
     * Must match mechanic_settings.advance_amount when advanceEnabled = true.
     */
    private BigDecimal advancePaid;

    private BigDecimal estimatedCost;
    private Integer estimatedDurationMinutes;
    private String customerNotes;
}
