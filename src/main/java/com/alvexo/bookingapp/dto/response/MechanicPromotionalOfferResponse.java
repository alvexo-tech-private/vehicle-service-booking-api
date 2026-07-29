package com.alvexo.bookingapp.dto.response;

import com.alvexo.bookingapp.model.DiscountType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MechanicPromotionalOfferResponse {

    private Long id;
    private Long mechanicId;
    private String title;
    private String description;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
