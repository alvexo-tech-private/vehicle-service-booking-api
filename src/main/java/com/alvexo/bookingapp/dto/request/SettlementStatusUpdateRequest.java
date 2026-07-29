package com.alvexo.bookingapp.dto.request;

import com.alvexo.bookingapp.model.SettlementStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Admin-only transition PENDING -> PROCESSING -> PAID
 * (not part of the mobile-facing spec — the app only reads settlements;
 * something has to be able to actually mark one paid once a real bank
 * transfer clears). Payment fields are required when moving to PAID.
 */
@Data
public class SettlementStatusUpdateRequest {

    @NotNull(message = "status is required")
    private SettlementStatus status;

    private String transactionNumber;
    private String paymentGateway;
    private String paymentGatewayReference;
    private LocalDate paymentDate;
    private LocalTime paymentTime;
    private String paymentStatus;
}
