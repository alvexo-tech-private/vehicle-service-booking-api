package com.alvexo.bookingapp.dto.response;

import com.alvexo.bookingapp.model.SettlementStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementResponse {

    private Long id;
    private LocalDate settlementDate;
    /** Total advance collected across the day's bookings. */
    private BigDecimal netPay;
    /** Flat advance-handling fee (field name kept for API compatibility — no longer a penalty). */
    private BigDecimal serviceReliabilityAdjustment;
    private BigDecimal settlementReleased;
    private SettlementStatus status;

    /** Present only when status = PAID. */
    private String transactionNumber;
    private String paymentGateway;
    private String paymentGatewayReference;
    private LocalDate paymentDate;
    private LocalTime paymentTime;
    private String paymentStatus;

    private String workshopName;
    private String workshopId;
}
