package com.alvexo.bookingapp.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MechanicSettingsResponse {

    private Long id;
    private Long mechanicId;
    private String mechanicName;

    private Integer maxVehiclesPerDay;
    private Boolean reserveCapacity;
    private BigDecimal fullDayCapacityHours;
    private String jobCardSerialPrefix;

    private LocalTime serviceReportingTime;
    private LocalTime expressReportingTime;

    private Boolean advanceEnabled;
    private BigDecimal advanceAmount;

    private List<MechanicServiceSettingResponse> serviceSettings;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
