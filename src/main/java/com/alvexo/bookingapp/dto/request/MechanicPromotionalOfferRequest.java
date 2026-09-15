package com.alvexo.bookingapp.dto.request;

import com.alvexo.bookingapp.model.DiscountType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class MechanicPromotionalOfferRequest {

    @NotBlank(message = "title is required")
    @Size(max = 150)
    private String title;

    private String description;

    @NotNull(message = "discountType is required")
    private DiscountType discountType;

    @NotNull(message = "discountValue is required")
    @DecimalMin(value = "0.01", message = "discountValue must be greater than 0")
    private BigDecimal discountValue;

    @NotNull(message = "startDate is required")
    private LocalDate startDate;

    @NotNull(message = "endDate is required")
    private LocalDate endDate;

    private Boolean active = true;
}
