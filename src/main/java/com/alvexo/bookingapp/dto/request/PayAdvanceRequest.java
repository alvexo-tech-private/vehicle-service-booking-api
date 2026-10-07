package com.alvexo.bookingapp.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** POST /api/bookings/{id}/pay-advance body — rider reports a completed advance payment. */
@Data
public class PayAdvanceRequest {

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01", message = "amount must be positive")
    private BigDecimal amount;

    @NotBlank(message = "paymentTransactionId is required")
    private String paymentTransactionId;
}
