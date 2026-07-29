package com.alvexo.bookingapp.dto.response;

import com.alvexo.bookingapp.model.EarningKind;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Earnings tab (§5.1) row — positive amount = advance, negative = cancellation deduction. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EarningEntryResponse {
    private String bookingId;
    private String vehicleNumber;
    private BigDecimal amount;
    private EarningKind kind;
}
