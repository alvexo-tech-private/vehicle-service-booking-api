package com.alvexo.bookingapp.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One service's per-date capacity override, keyed by service name (matches {@code MechanicServiceSetting.serviceName}). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceCapacityOverrideEntry {

    @NotBlank(message = "serviceType is required")
    private String serviceType;

    @NotNull(message = "capacity is required")
    @Min(value = 0, message = "capacity must be at least 0")
    private Integer capacity;
}
