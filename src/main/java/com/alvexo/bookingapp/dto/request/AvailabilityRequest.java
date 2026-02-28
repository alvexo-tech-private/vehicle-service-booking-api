package com.alvexo.bookingapp.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalTime;

import com.alvexo.bookingapp.model.DayOfWeek;

@Data
public class AvailabilityRequest {
    @NotNull(message = "Day of week is required")
    private DayOfWeek dayOfWeek;
    
    @NotNull(message = "Start time is required")
    private LocalTime startTime;
    
    @NotNull(message = "End time is required")
    private LocalTime endTime;
    
    private Boolean isAvailable;
    private Integer slotDurationMinutes;
}
