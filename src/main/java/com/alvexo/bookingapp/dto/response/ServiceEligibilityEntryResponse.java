package com.alvexo.bookingapp.dto.response;

import java.util.List;

import com.alvexo.bookingapp.model.FuelType;
import com.alvexo.bookingapp.model.ServiceCategory;

public record ServiceEligibilityEntryResponse(
        FuelType fuelType,
        ServiceCategory serviceCategory,
        List<String> vehicleMakes,
        boolean otherMakesAllowed
) {}
