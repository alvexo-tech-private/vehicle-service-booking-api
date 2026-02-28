package com.alvexo.bookingapp.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.alvexo.bookingapp.model.ServiceType;

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
    
    private BigDecimal estimatedCost;
    private Integer estimatedDurationMinutes;
    private String customerNotes;
}
