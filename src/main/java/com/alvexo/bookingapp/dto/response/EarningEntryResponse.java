package com.alvexo.bookingapp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Daily Advance Summary (BACKEND_REQUIREMENTS_FULL_APP_WORKSHOP_RIDER.md §9) per-vehicle row. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EarningEntryResponse {
    private String bookingId;
    private String registrationNumber;
    private BigDecimal advancePaid;
}
