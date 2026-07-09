package com.alvexo.bookingapp.dto.response;

import com.alvexo.bookingapp.model.JobCardType;
import com.alvexo.bookingapp.model.WorkshopClassification;
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

    private JobCardType jobCardType;
    private WorkshopClassification classification;

    private Integer maxVehiclesPerDay;
    private Boolean reserveCapacity;
    private Boolean reserveForSlots;
    private BigDecimal fullDayCapacityHours;
    private String jobCardSerialPrefix;

    private LocalTime serviceReportingTime;
    private LocalTime expressReportingTime;

    private Boolean advanceEnabled;
    private BigDecimal advanceAmount;

    private Boolean autoAllocationEnabled;
    private BigDecimal autoAllocationCapacityHours;

    private List<MechanicServiceSettingResponse> serviceSettings;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
