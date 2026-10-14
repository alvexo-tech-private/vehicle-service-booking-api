package com.alvexo.bookingapp.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class ServiceEligibilityRequest {

    @NotEmpty(message = "entries must not be empty")
    @Valid
    private List<ServiceEligibilityEntryRequest> entries;
}
