package com.alvexo.bookingapp.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MechanicDailyOverrideResponse {

    private Long id;
    private LocalDate date;
    private Integer maxVehiclesPerDayOverride;
    private BigDecimal fullDayCapacityHoursOverride;
    private Boolean advanceEnabledOverride;
    private BigDecimal advanceAmountOverride;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
