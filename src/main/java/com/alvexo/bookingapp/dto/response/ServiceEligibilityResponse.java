package com.alvexo.bookingapp.dto.response;

import java.util.List;

public record ServiceEligibilityResponse(
        List<ServiceEligibilityEntryResponse> entries
) {}
