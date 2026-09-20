package com.alvexo.bookingapp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Daily Advance Summary — GET /api/service-desk/earnings
 * (BACKEND_REQUIREMENTS_FULL_APP_WORKSHOP_RIDER.md §9). Zero-advance vehicles remain in
 * {@code bookings}; cancelled/rejected/pending bookings are excluded entirely.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EarningsResponse {
    private LocalDate serviceDate;
    private int totalVehicles;
    private int vehiclesWithAdvance;
    private int vehiclesWithoutAdvance;
    private BigDecimal totalAdvance;
    private BigDecimal feePerAdvanceBooking;
    private BigDecimal totalFee;
    private BigDecimal netPaymentToWorkshop;
    private List<EarningEntryResponse> bookings;
}
