package com.alvexo.bookingapp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * GET /api/service-desk/earnings (§5.2/§5.3). Server-computed totals are
 * included so both the Summary and Vehicle-wise tabs can render from one
 * response; entries carries the raw per-vehicle rows for the Vehicle-wise tab.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EarningsResponse {
    private BigDecimal advances;
    private BigDecimal cancellations;
    private BigDecimal net;
    private List<EarningEntryResponse> entries;
}
