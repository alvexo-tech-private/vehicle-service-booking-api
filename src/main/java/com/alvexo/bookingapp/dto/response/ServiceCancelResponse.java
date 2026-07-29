package com.alvexo.bookingapp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response of a Today/Service Week cancel action (§1.7/§2.5). reliabilityAdjustment
 * is populated only when the cancellation happened past the reschedule cutoff.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceCancelResponse {
    private String bookingId;
    private String cancellationMessage;
    private BigDecimal reliabilityAdjustment;
}
