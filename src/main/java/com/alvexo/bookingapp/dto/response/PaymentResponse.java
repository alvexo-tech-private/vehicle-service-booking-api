package com.alvexo.bookingapp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.alvexo.bookingapp.model.PaymentStatus;
import com.alvexo.bookingapp.model.PaymentType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private Long id;
    private PaymentType paymentType;
    private BigDecimal amount;
    private String currency;
    private PaymentStatus status;
    private String transactionId;
    private LocalDateTime paymentDate;
    private LocalDateTime createdAt;
}
