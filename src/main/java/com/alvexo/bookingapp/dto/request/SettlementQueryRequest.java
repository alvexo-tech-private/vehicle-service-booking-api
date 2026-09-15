package com.alvexo.bookingapp.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/** POST /api/settlements/queries (WORKSHOP_FINANCE_API_SPEC.md §6). */
@Data
public class SettlementQueryRequest {

    @NotNull(message = "settlementDate is required")
    private LocalDate settlementDate;

    @NotBlank(message = "description is required")
    @Size(max = 500, message = "description must be at most 500 characters")
    private String description;
}
