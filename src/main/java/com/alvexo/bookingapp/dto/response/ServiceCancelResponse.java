package com.alvexo.bookingapp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response of a Today/Service Week cancel action (§1.7/§2.5). No cancellation penalty is
 * applied — cancelling never affects settlement/earnings.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceCancelResponse {
    private String bookingId;
    private String cancellationMessage;
}
