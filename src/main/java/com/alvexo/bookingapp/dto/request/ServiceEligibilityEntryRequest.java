package com.alvexo.bookingapp.dto.request;

import java.util.List;

import com.alvexo.bookingapp.model.FuelType;
import com.alvexo.bookingapp.model.ServiceCategory;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ServiceEligibilityEntryRequest {

    @NotNull(message = "fuelType is required")
    private FuelType fuelType;

    @NotNull(message = "serviceCategory is required")
    private ServiceCategory serviceCategory;

    /** Vehicle make names this workshop services for this (fuelType, serviceCategory) pair. */
    private List<String> vehicleMakes;

    /** When true, makes not in vehicleMakes are still accepted (open policy). */
    private Boolean otherMakesAllowed;
}
