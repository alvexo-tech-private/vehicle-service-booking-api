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
    private BigDecimal netPay;
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
