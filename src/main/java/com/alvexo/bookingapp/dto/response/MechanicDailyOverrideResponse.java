package com.alvexo.bookingapp.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.alvexo.bookingapp.dto.ServiceCapacityOverrideEntry;

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
    private List<ServiceCapacityOverrideEntry> serviceCapacities;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
